package org.tmoerman.cases.ghb

import breeze.linalg.SparseVector
import breeze.linalg.SparseVector.zeros
import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.BreezeConversions._
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Colour.{AverageFilterValue, ClusterMaxFrequency, ClusterSize}
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering.ClusteringParams
import org.tmoerman.plongeur.tda.cluster.Scale.histogram
import org.tmoerman.plongeur.tda.geometry.Laplacian
import org.tmoerman.plongeur.tda.knn.ExactKNN
import org.tmoerman.plongeur.tda.knn.ExactKNN.ExactKNNParams
import org.tmoerman.plongeur.tda.{Filters, Brewer, TDAMachine}
import org.tmoerman.plongeur.test.SparkContextSpec
import org.tmoerman.plongeur.util.TimeUtils
import rx.lang.scala.subjects.PublishSubject

/**
  * @author Thomas Moerman
  */
class ZeiselSpec extends FlatSpec with SparkContextSpec with Matchers {

  import ZeiselReader._

  val wd = "/Users/tmo/Work/ghb2016/data/zeisel/"

  /**
    * See: http://linnarssonlab.org/cortex/
    *
    * line size = 3007 - 2 = 3005 (first two cols are meta)
    *
    * 0: tissue -> [sscortex, ca1hippocampus, ...]
    * 1: group: Int
    * 2: total mRNA mol: Int
    * 3: well: Int
    * 4: sex: {-1, 1}
    * 5: age: Int
    * 6: diameter: Int
    * 7: cell ID: String
    * 8: level1class: String
    * 9: level2class: String
    * 10: ---- empty ----
    *
    * 11..end: problem: indices do not line up -> (for mRNA, column 2 indicates group assignment of genes in the level 1 BackSPIN analysis).
    */
  val exp_mRNA = wd + "expression_mRNA_17-Aug-2014.txt"

  val L = Some(100)

  val rdd = parseZeisel(sc, exp_mRNA, limit=L)

  val ctx = TDAContext(sc, rdd)

  val dist = TanimotoDistance

  "ExactKNN on the (partial) Zeisel dataset" should "work" ignore {
    val (knnRDD, duration) = TimeUtils.time{
      val kNNParams = ExactKNNParams(k = 20, distance = dist)

      ExactKNN(ctx, kNNParams).collect
    }

    println(s"Zeisel top $L exact kNN: ${duration.toMinutes} minutes")
  }

  "Laplacian eigenvectors for Zeisel dataset" should "work" ignore {
    val (lapEig, duration) = TimeUtils.time{
      Laplacian.tanimoto(ctx).collect
    }

    println(s"Zeisel top $L laplacian eigenvectors: ${duration.toMinutes} minutes")

    println(lapEig.head)
  }

  "TDA on the (partial) Zeisel dataset)" should "work" ignore {

    val lap0 = Filter(LaplacianEigenVector(0, distance = TanimotoDistance), 30, 0.3)
    val lap1 = Filter(LaplacianEigenVector(1, distance = TanimotoDistance), 30, 0.3)
    val den = Filter(Density(sigma=1.0, distance = dist), 45, 0.30)
    val ecc = Filter(Eccentricity(Right(INFINITY), distance = dist), 45, 0.30)

    val pc0  = Filter(PrincipalComponent(0), 45, 0.3)
    val pc1  = Filter(PrincipalComponent(1), 30, 0.3)
    val mean = Filter(FeatureMean, 30, 0.3)
    val vari = Filter(FeatureVariance, 30, 0.3)
    val age  = Filter(Meta("age"), 30, 0.3)
    val diam = Filter(Meta("diameter"), 30, 0.3)
    val mRNA = Filter(Meta("total mRNA mol"), 30, 0.3)

    val bySex    = ClusterMaxFrequency(Brewer.palettes("Set1")(3), (d: DataPoint) => d.meta.get("sex"))
    val byTissue = ClusterMaxFrequency(Brewer.palettes("Set1")(4), (d: DataPoint) => d.meta.get("tissue"))
    val byLevel1 = ClusterMaxFrequency(Brewer.palettes("Set1")(7), (d: DataPoint) => d.meta.get("level1class"))
    val byGroup  = ClusterMaxFrequency(Brewer.palettes("Set1")(7), (d: DataPoint) => d.meta.get("group"))

    val clSize = ClusterSize(Brewer.palettes("RdYlBu")(9).reverse)

    val avgEcc = AverageFilterValue(Brewer.palettes("Blues")(9), ecc)
    val avgDen = AverageFilterValue(Brewer.palettes("Reds")(9), den)
    val avgAge = AverageFilterValue(Brewer.palettes("RdYlBu")(9), age)
    val avgDia = AverageFilterValue(Brewer.palettes("RdYlBu")(9), diam)
    val avgRNA = AverageFilterValue(Brewer.palettes("RdYlBu")(9), mRNA)

    val BASE =
      TDAParams(
        lens = TDALens(lap0),
        clusteringParams = ClusteringParams(distance = dist),
        scaleSelection = histogram(30),
        collapseDuplicateClusters = true,
        colouring = clSize)

    val in$ = PublishSubject[TDAParams]

    val out$ = TDAMachine.run(ctx, in$).toVector

    out$.subscribe(_.map(r => print(r.clusters.mkString("\n"))))

    in$.onNext(BASE)
    in$.onCompleted()

    import org.tmoerman.plongeur.util.RxUtils._

    waitFor(out$)
  }

}

object ZeiselReader {

  type E = (Index, Either[(Int, Double), (String, Any)])

  val N_OFFSET = 2
  val D_OFFSET = 11

  def toMeta(columns: List[(String, Int)],
             limit: Option[Int],
             f: (String => Any) = identity) = (columns: @unchecked) match {

    case _ :: (label, _) :: values =>
      limit.map(values.take).getOrElse(values).map{ case (value, idx) => (idx - N_OFFSET, Right((label, f(value)))) }
  }

  def toFeatures(lineIndex: Int,
                 columns: List[(String, Int)],
                 limit: Option[Int]) = (columns: @unchecked) match {

    case _ :: _ :: features => limit.map(features.take).getOrElse(features).flatMap{ case (feature, idx) => {
      val value = feature.toDouble

      if (value > 0) (idx - N_OFFSET, Left((lineIndex - D_OFFSET, value))) :: Nil else Nil
    }}
  }

  def parseLine(lineIdx: Int, columns: List[(String, Int)], limit: Option[Int]): Seq[E] = lineIdx match {
    case 0 => toMeta(columns, limit)              // tissue
    case 1 => toMeta(columns, limit, _.toInt)     // group
    case 2 => toMeta(columns, limit, _.toInt)     // total mRNA mol
    case 3 => toMeta(columns, limit, _.toInt)     // well
    case 4 => toMeta(columns, limit, _.toInt)     // sex
    case 5 => toMeta(columns, limit, _.toInt)     // age
    case 6 => toMeta(columns, limit, _.toDouble)  // diameter
    case 7 => Nil //toMeta(columns, limit)              // cell ID
    case 8 => toMeta(columns, limit)              // level 1 class
    case 9 => toMeta(columns, limit)              // level 2 class
    case 10 => Nil                                // empty line
    case _ => toFeatures(lineIdx, columns, limit) // feature
  }

  def parseZeisel(sc: SparkContext, file: String, limit: Option[Int] = None) = {

    lazy val N = sc.textFile(file).map(line => line.split("\t").length).first - N_OFFSET

    val D = sc.textFile(file).count.toInt - D_OFFSET

    type ACC = (SparseVector[Double], Map[String, Any])

    val INIT: ACC = (zeros[Double](D), Map.empty)

    sc
      .textFile(file)
      .zipWithIndex
      .flatMap{ case (line, lineIdx) =>
        val columns = line.split("\t").map(trim).zipWithIndex.toList
        parseLine(lineIdx.toInt, columns, limit) }
      .aggregateByKey(INIT)(
        { case ((sparse, meta), e) => e match {
            case Left((idx, v)) => sparse.update(idx, v); (sparse, meta)
            case Right((key, v)) => (sparse, meta + (key -> v))
        }},
        { case ((sparse1, meta1), (sparse2, meta2)) => (sparse1 + sparse2, meta1 ++ meta2) })
      .map { case (idx, (sparse, meta)) => DataPoint(idx, sparse.toMLLib, Some(meta)) }
  }

}