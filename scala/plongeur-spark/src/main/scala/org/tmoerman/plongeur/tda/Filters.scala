package org.tmoerman.plongeur.tda

import java.lang.Math.{exp, min}

import breeze.linalg.{DenseVector => BDV}
import breeze.{linalg, stats}
import org.apache.spark.mllib.feature.PCA
import org.apache.spark.mllib.linalg.BreezeConversions._
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances.{Distance, DistanceFunction, TanimotoDistance}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.geometry.Laplacian
import org.tmoerman.plongeur.util.RDDFunctions._

import scala.math._

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable {

  // TODO delegate to its own package
  // TODO extract Eccentricity and Density

  type FilterValue      = Double
  type FilterRDD        = RDD[(Index, FilterValue)]
  type FilterRDDFactory = (FilterSpec) => FilterRDD
  type LevelSetsRDD     = RDD[(Index, Seq[LevelSetID])]

  val MAX_PCs: Int = 10

  def toContextAmendment(filter: Filter) =
    toSketchAmendment(filter) andThen
    // toKNNAmendment(filter) andThen // TODO for now we expect a knn data structure to be present in the TDAContext  knn cache
    toFilterAmendment(filter)

  /**
    * @param rdd
    * @return Returns the (min, max) boundaries of the specified FilterRDD.
    */
  def boundaries(rdd: FilterRDD) = {
    val values = rdd.map(_._2).cache
    (values.min, values.max)
  }

  /**
    * @param filter
    * @return Returns a ContextAmendment function that updates both the specified TDAContext's sketches and broadcasts
    *         in function of the
    */
  def toSketchAmendment(filter: Filter): ContextAmendment = (ctx: TDAContext) => {
    val maybeAmended = for {
      sketchKey <- toSketchKey(filter)

      ctx1 <- ctx.sketchCache
                 .get(sketchKey)
                 .orElse(filter.sketch.map(params => Sketch(ctx, params)))
                 .map(sketch => ctx.copy(sketchCache = ctx.sketchCache + (sketchKey -> sketch)))

      ctx2 <- ctx1.broadcasts
                  .get(sketchKey)
                  .orElse(ctx1.sketchCache.get(sketchKey).map(sketch => ctx1.sc.broadcast(sketch.lookupMap)))
                  .map(value => ctx1.copy(broadcasts = ctx1.broadcasts + (sketchKey -> value)))

    } yield ctx2

    maybeAmended.getOrElse(ctx)
  }

  /**
    * @param filter
    * @return Returns a TDAContext amendment function.
    */
  def toFilterAmendment(filter: Filter, filterRDDFactory: Option[FilterRDDFactory] = None): ContextAmendment = (ctx: TDAContext) =>
    ctx
      .filterCache
      .get(filter.spec.key)
      .orElse(filterRDDFactory.orElse(Some(toFilterRDDFactory(filter, ctx))))
      .map(filterRDDFactory => ctx.copy(filterCache = ctx.filterCache + (filter.spec.key -> filterRDDFactory)))
      .getOrElse(ctx)

  /**
    * @param filter
    * @param ctx
    * @return Returns a FilterRDD factory function.
    */
  def toFilterRDDFactory(filter: Filter, ctx: TDAContext): FilterRDDFactory = {
    import filter._

    // TODO: fold in sketch/coreset stuff
    // TODO: val ctxLike: ContextLike = toSketchKey(filter).flatMap(key => ctx.sketchCache.get(key)).getOrElse(ctx)

    spec match {

      case Meta(key: String) =>
        val result = ctx.dataPoints.map(p => {
          val value = p.meta.get(key) match {
            case f: Float  => f.toDouble
            case d: Double => d
            case i: Int    => i.toDouble
            case s: String => s.toDouble
          }

          (p.index, value)
        })

        {case _: FilterSpec => result.cache }

      case Feature(n) =>
        val result = ctx.dataPoints.map(p => (p.index, p.features(n)))

        { case _: FilterSpec => result.cache }

      case FeatureMin =>
        val result = ctx.dataPoints.map(p => (p.index, linalg.min(p.features.toBreeze)))

        { case _: FilterSpec => result.cache }

      case FeatureMax =>
        val result = ctx.dataPoints.map(p => (p.index, linalg.max(p.features.toBreeze)))

        { case _: FilterSpec => result.cache }

      case FeatureMean =>
        val result = ctx.dataPoints.map(p => (p.index, stats.mean(p.features.toBreeze)))

        { case _: FilterSpec => result.cache }

      case FeatureVariance =>
        val result = ctx.dataPoints.map(p => (p.index, stats.variance(p.features.toBreeze)))

        { case _: FilterSpec => result.cache }

      case FeatureStDev =>
        val result = ctx.dataPoints.map(p => (p.index, stats.stddev(p.features.toBreeze)))

        { case _: FilterSpec => result.cache }

      case Eccentricity(p, distance) =>
        val result = eccentricityRDD(ctx, p, distance)

        { case _: FilterSpec => result.cache }

      case Density(sigma, distance) =>
        val result = densityRDD(ctx, sigma, distance)

        { case _: FilterSpec => result.cache }

      case _: PrincipalComponent =>
        val pcaModel = new PCA(min(MAX_PCs, ctx.D)).fit(ctx.dataPoints.map(_.features))

        { case PrincipalComponent(n) => ctx.dataPoints.map(p => (p.index, pcaModel.transform(p.features)(n))) }

      // TODO temporary
      case LaplacianEigenVector(_, _, sigma, TanimotoDistance) =>

        val laplacianEigenVectors = Laplacian.tanimoto(ctx, sigma)

      { case LaplacianEigenVector(n, _, _, _) => laplacianEigenVectors.mapValues(_(n)) }

      case LaplacianEigenVector(_, k, sigma, distance) =>
        val knnRDD = ctx.knnCache.getOrElse(toKNNKey(spec), throw new IllegalStateException(s"No KNN found for $distance"))

        val laplacianEigenVectors = Laplacian.apply(ctx, knnRDD, k, sigma)

        { case LaplacianEigenVector(n, _, _, _) => laplacianEigenVectors.mapValues(_(n)) }

    }
  }

  def toSketchKey(filter: Filter): Option[CacheKey] = filter.sketch

  def toKNNKey(filter: Filter): Option[CacheKey] = filter.spec match {
    case LaplacianEigenVector(_,_,_, distance) => Some(distance)
    case _                                     => None
  }

  /**
    * @param p
    * @param ctx
    * @param distance
    * @return Returns an RDD by Index to the L_n eccentricity of that point.
    *         L_n eccentricity assigns to each point the distance to the point most distant from it.
    *
    *         See: Extracting insights from the shape of complex data using topology
    *              -- P. Y. Lum, G. Singh, [...], and G. Carlsson
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  def eccentricityRDD(ctx: ContextLike, p: Either[Index, _], distance: DistanceFunction): FilterRDD = {
    val N = ctx.N

    p match {

      case Right(INFINITY) =>
        unfoldDistances(ctx, distance)
          .reduceByKey(max)

      case Left(1) =>
        unfoldDistances(ctx, distance)
          .reduceByKey(_ + _)
          .map{ case (i, sum) => (i, sum / N) }

      case Left(n) =>
        unfoldDistances(ctx, distance)
          .mapValues(d => pow(d, n))
          .reduceByKey(_ + _)
          .map{ case (i, sum) => (i, pow(sum, 1d / n) / N) }

      case _ =>
        throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$p'")
    }
  }

  /**
    * TODO: see https://en.wikipedia.org/wiki/Multivariate_kernel_density_estimation
    *
    * @param sigma
    * @param ctx
    * @param distance
    * @return TODO update documentation
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  def densityRDD(ctx: ContextLike, sigma: Distance, distance: DistanceFunction): FilterRDD = {
    val N = ctx.N
    val D = ctx.D

    val denominator = 2 * sigma * sigma

    unfoldDistances(ctx, distance)
      .mapValues(d => exp(-pow(d, 2) / denominator))
      .reduceByKey(_ + _)
      //.mapValues(_ / (N * pow(sqrt(2 * PI * sigma), D))) -> TODO: useless denominator?
  }

  private def unfoldDistances(ctx: ContextLike, distance: DistanceFunction): RDD[(Index, Distance)] =
    ctx
      .dataPoints
      .distinctComboPairs
      .flatMap { case (p, q) => val d = distance(p, q); (p.index, d) ::(q.index, d) :: Nil }

}