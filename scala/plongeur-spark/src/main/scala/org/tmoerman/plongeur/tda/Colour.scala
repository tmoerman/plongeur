package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.IterableFunctions._
import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * @author Thomas Moerman
  */
object Colour extends Serializable {

  type Size      = Int
  type RGB       = String
  type Name      = String
  type ColourBin = Int
  type Palette   = Array[RGB]

  type ContinuousPalette = (Double) => RGB // TODO later

  // TODO take into account negative filter values!

  val DEFAULT_NR_BINS = 3

  trait Colouring extends (TDAContext => RDD[Cluster] => RDD[Cluster])

  /**
    * A Colouring that maps the average filter value of the datapoints in a cluster to a colour bin.
    *
    * @param palette
    * @param filter
    */
  case class AverageFilterValue(palette: Palette, filter: Filter) extends Colouring {

    override def apply(ctx: TDAContext) = (rdd: RDD[Cluster]) => {
      val filterRDD = ctx.filterCache(toFilterKey(filter)).apply(filter.spec)

      val min = filterRDD.values.min

      val filterRDDCorrected = filterRDD.mapValues(_ - min)

      val clusterPointsByIndex =
        rdd.flatMap(cluster => cluster.dataPoints.map(p => (p.index, cluster.id)))

      val averageByCluster =
        (filterRDDCorrected cogroup clusterPointsByIndex)
          .flatMap{ case (p, (vs, cs)) => cs.map(c => (c, vs.head)) }
          .averageByKey

      val maxAvg = averageByCluster.values.max

      (rdd.keyBy(_.id) join averageByCluster)
        .map{ case (_, (cluster, avg)) =>
          cluster.copy(colour = Some(avg / maxAvg).map(toBin(palette.size)).map(palette)) }
    }

  }

  /**
    * A Colouring that maps the percentage of points in a Cluster to a colour bin.
    *
    * @param palette
    * @param predicate
    */
  case class ClusterPercentage(palette: Palette,
                               predicate: (DataPoint) => Boolean) extends Colouring {

    override def apply(ctx: TDAContext) = (rdd: RDD[Cluster]) => {

      def pctPositive(cluster: Cluster) = cluster.dataPoints.count(predicate).toDouble / cluster.size

      rdd.map(cluster => cluster.copy(colour = Some(pctPositive(cluster)).map(toBin(palette.size)).map(palette)))
    }

  }

  def toBin(nrBins: Int)(pct: Double) = (pct * nrBins).toInt

  /**
    * A Colouring that maps a cluster to the first (0) bin if it has any DataPoint instances that are
    * matched by the specified selector.
    *
    * @param palette
    * @param predicate
    */
  case class LocalOccurrence(palette: Palette, predicate: (DataPoint) => Boolean) extends Colouring {

    override def apply(ctx: TDAContext) = (rdd: RDD[Cluster]) => {

      def positive(cluster: Cluster) = cluster.dataPoints.exists(predicate)

      rdd.map(cluster => {
        val bin = if (positive(cluster)) Some(0) else None

        cluster.copy(colour = bin.map(palette))
      })
    }

  }

  /**
    * A Colouring that maps a cluster to the bin corresponding to the category of which the cluster has
    * the most DataPoint instances.
    *
    * @param projection
    * @param palette
    */
  case class ClusterMaxFrequency(palette: Palette, projection: (DataPoint) => Any) extends Colouring {

    override def apply(ctx: TDAContext) = (rdd: RDD[Cluster]) => {

      val attributesRDD =
        rdd
          .flatMap(_.dataPoints.map(p => projection(p)))
          .distinct
          .zipWithIndex
          .map{ case (any, l) => (any, l.toInt) }

      val attributesToBin: Map[Any, ColourBin] =
        attributesRDD
          .collect
          .toMap

      require(palette.size >= attributesToBin.size, s"palette size ${palette.size} <  nr distinct attributes${attributesToBin.size}")

      def maxFreqProjection(cluster: Cluster) = cluster.dataPoints.map(projection).frequencies.maxBy(_._2)._1

      rdd.map(cluster => cluster.copy(colour = Some(maxFreqProjection(cluster)).map(attributesToBin).map(palette)))
    }

  }

  /**
    * A Binner that maps every cluster to a specified bin.
    *
    * @param palette
    * @param bin
    */
  case class Constantly(palette: Palette, bin: ColourBin = 0) extends Colouring {

    override def apply(ctx: TDAContext) = (rdd: RDD[Cluster]) => rdd.map(_.copy(colour = Some(bin).map(palette)))

  }

  /**
    * A Binner that maps every cluster to no bins.
    */
  case class Nop() extends Colouring {

    override def apply(ctx: TDAContext) = (rdd: RDD[Cluster]) => rdd.map(_.copy(colour = None))

  }

}

object Brewer extends Serializable {

  val palettes = Map(

    "YlGn" -> Map(
      3 -> Array("#f7fcb9","#addd8e","#31a354"),
      4 -> Array("#ffffcc","#c2e699","#78c679","#238443"),
      5 -> Array("#ffffcc","#c2e699","#78c679","#31a354","#006837"),
      6 -> Array("#ffffcc","#d9f0a3","#addd8e","#78c679","#31a354","#006837"),
      7 -> Array("#ffffcc","#d9f0a3","#addd8e","#78c679","#41ab5d","#238443","#005a32"),
      8 -> Array("#ffffe5","#f7fcb9","#d9f0a3","#addd8e","#78c679","#41ab5d","#238443","#005a32"),
      9 -> Array("#ffffe5","#f7fcb9","#d9f0a3","#addd8e","#78c679","#41ab5d","#238443","#006837","#004529")),

    "YlGnBu" -> Map(
      3 -> Array("#edf8b1","#7fcdbb","#2c7fb8"),
      4 -> Array("#ffffcc","#a1dab4","#41b6c4","#225ea8"),
      5 -> Array("#ffffcc","#a1dab4","#41b6c4","#2c7fb8","#253494"),
      6 -> Array("#ffffcc","#c7e9b4","#7fcdbb","#41b6c4","#2c7fb8","#253494"),
      7 -> Array("#ffffcc","#c7e9b4","#7fcdbb","#41b6c4","#1d91c0","#225ea8","#0c2c84"),
      8 -> Array("#ffffd9","#edf8b1","#c7e9b4","#7fcdbb","#41b6c4","#1d91c0","#225ea8","#0c2c84"),
      9 -> Array("#ffffd9","#edf8b1","#c7e9b4","#7fcdbb","#41b6c4","#1d91c0","#225ea8","#253494","#081d58")),

    "GnBu" -> Map(
      3 -> Array("#e0f3db","#a8ddb5","#43a2ca"),
      4 -> Array("#f0f9e8","#bae4bc","#7bccc4","#2b8cbe"),
      5 -> Array("#f0f9e8","#bae4bc","#7bccc4","#43a2ca","#0868ac"),
      6 -> Array("#f0f9e8","#ccebc5","#a8ddb5","#7bccc4","#43a2ca","#0868ac"),
      7 -> Array("#f0f9e8","#ccebc5","#a8ddb5","#7bccc4","#4eb3d3","#2b8cbe","#08589e"),
      8 -> Array("#f7fcf0","#e0f3db","#ccebc5","#a8ddb5","#7bccc4","#4eb3d3","#2b8cbe","#08589e"),
      9 -> Array("#f7fcf0","#e0f3db","#ccebc5","#a8ddb5","#7bccc4","#4eb3d3","#2b8cbe","#0868ac","#084081")),

    "BuGn" -> Map(
      3 -> Array("#e5f5f9","#99d8c9","#2ca25f"),
      4 -> Array("#edf8fb","#b2e2e2","#66c2a4","#238b45"),
      5 -> Array("#edf8fb","#b2e2e2","#66c2a4","#2ca25f","#006d2c"),
      6 -> Array("#edf8fb","#ccece6","#99d8c9","#66c2a4","#2ca25f","#006d2c"),
      7 -> Array("#edf8fb","#ccece6","#99d8c9","#66c2a4","#41ae76","#238b45","#005824"),
      8 -> Array("#f7fcfd","#e5f5f9","#ccece6","#99d8c9","#66c2a4","#41ae76","#238b45","#005824"),
      9 -> Array("#f7fcfd","#e5f5f9","#ccece6","#99d8c9","#66c2a4","#41ae76","#238b45","#006d2c","#00441b")),

    "PuBuGn" -> Map(
      3 -> Array("#ece2f0","#a6bddb","#1c9099"),
      4 -> Array("#f6eff7","#bdc9e1","#67a9cf","#02818a"),
      5 -> Array("#f6eff7","#bdc9e1","#67a9cf","#1c9099","#016c59"),
      6 -> Array("#f6eff7","#d0d1e6","#a6bddb","#67a9cf","#1c9099","#016c59"),
      7 -> Array("#f6eff7","#d0d1e6","#a6bddb","#67a9cf","#3690c0","#02818a","#016450"),
      8 -> Array("#fff7fb","#ece2f0","#d0d1e6","#a6bddb","#67a9cf","#3690c0","#02818a","#016450"),
      9 -> Array("#fff7fb","#ece2f0","#d0d1e6","#a6bddb","#67a9cf","#3690c0","#02818a","#016c59","#014636")),

    "PuBu" -> Map(
      3 -> Array("#ece7f2","#a6bddb","#2b8cbe"),
      4 -> Array("#f1eef6","#bdc9e1","#74a9cf","#0570b0"),
      5 -> Array("#f1eef6","#bdc9e1","#74a9cf","#2b8cbe","#045a8d"),
      6 -> Array("#f1eef6","#d0d1e6","#a6bddb","#74a9cf","#2b8cbe","#045a8d"),
      7 -> Array("#f1eef6","#d0d1e6","#a6bddb","#74a9cf","#3690c0","#0570b0","#034e7b"),
      8 -> Array("#fff7fb","#ece7f2","#d0d1e6","#a6bddb","#74a9cf","#3690c0","#0570b0","#034e7b"),
      9 -> Array("#fff7fb","#ece7f2","#d0d1e6","#a6bddb","#74a9cf","#3690c0","#0570b0","#045a8d","#023858")),

    "BuPu" -> Map(
      3 -> Array("#e0ecf4","#9ebcda","#8856a7"),
      4 -> Array("#edf8fb","#b3cde3","#8c96c6","#88419d"),
      5 -> Array("#edf8fb","#b3cde3","#8c96c6","#8856a7","#810f7c"),
      6 -> Array("#edf8fb","#bfd3e6","#9ebcda","#8c96c6","#8856a7","#810f7c"),
      7 -> Array("#edf8fb","#bfd3e6","#9ebcda","#8c96c6","#8c6bb1","#88419d","#6e016b"),
      8 -> Array("#f7fcfd","#e0ecf4","#bfd3e6","#9ebcda","#8c96c6","#8c6bb1","#88419d","#6e016b"),
      9 -> Array("#f7fcfd","#e0ecf4","#bfd3e6","#9ebcda","#8c96c6","#8c6bb1","#88419d","#810f7c","#4d004b")),

    "RdPu" -> Map(
      3 -> Array("#fde0dd","#fa9fb5","#c51b8a"),
      4 -> Array("#feebe2","#fbb4b9","#f768a1","#ae017e"),
      5 -> Array("#feebe2","#fbb4b9","#f768a1","#c51b8a","#7a0177"),
      6 -> Array("#feebe2","#fcc5c0","#fa9fb5","#f768a1","#c51b8a","#7a0177"),
      7 -> Array("#feebe2","#fcc5c0","#fa9fb5","#f768a1","#dd3497","#ae017e","#7a0177"),
      8 -> Array("#fff7f3","#fde0dd","#fcc5c0","#fa9fb5","#f768a1","#dd3497","#ae017e","#7a0177"),
      9 -> Array("#fff7f3","#fde0dd","#fcc5c0","#fa9fb5","#f768a1","#dd3497","#ae017e","#7a0177","#49006a")),

    "PuRd" -> Map(
      3 -> Array("#e7e1ef","#c994c7","#dd1c77"),
      4 -> Array("#f1eef6","#d7b5d8","#df65b0","#ce1256"),
      5 -> Array("#f1eef6","#d7b5d8","#df65b0","#dd1c77","#980043"),
      6 -> Array("#f1eef6","#d4b9da","#c994c7","#df65b0","#dd1c77","#980043"),
      7 -> Array("#f1eef6","#d4b9da","#c994c7","#df65b0","#e7298a","#ce1256","#91003f"),
      8 -> Array("#f7f4f9","#e7e1ef","#d4b9da","#c994c7","#df65b0","#e7298a","#ce1256","#91003f"),
      9 -> Array("#f7f4f9","#e7e1ef","#d4b9da","#c994c7","#df65b0","#e7298a","#ce1256","#980043","#67001f")),

    "OrRd" -> Map(
      3 -> Array("#fee8c8","#fdbb84","#e34a33"),
      4 -> Array("#fef0d9","#fdcc8a","#fc8d59","#d7301f"),
      5 -> Array("#fef0d9","#fdcc8a","#fc8d59","#e34a33","#b30000"),
      6 -> Array("#fef0d9","#fdd49e","#fdbb84","#fc8d59","#e34a33","#b30000"),
      7 -> Array("#fef0d9","#fdd49e","#fdbb84","#fc8d59","#ef6548","#d7301f","#990000"),
      8 -> Array("#fff7ec","#fee8c8","#fdd49e","#fdbb84","#fc8d59","#ef6548","#d7301f","#990000"),
      9 -> Array("#fff7ec","#fee8c8","#fdd49e","#fdbb84","#fc8d59","#ef6548","#d7301f","#b30000","#7f0000")),

    "YlOrRd" -> Map(
      3 -> Array("#ffeda0","#feb24c","#f03b20"),
      4 -> Array("#ffffb2","#fecc5c","#fd8d3c","#e31a1c"),
      5 -> Array("#ffffb2","#fecc5c","#fd8d3c","#f03b20","#bd0026"),
      6 -> Array("#ffffb2","#fed976","#feb24c","#fd8d3c","#f03b20","#bd0026"),
      7 -> Array("#ffffb2","#fed976","#feb24c","#fd8d3c","#fc4e2a","#e31a1c","#b10026"),
      8 -> Array("#ffffcc","#ffeda0","#fed976","#feb24c","#fd8d3c","#fc4e2a","#e31a1c","#b10026"),
      9 -> Array("#ffffcc","#ffeda0","#fed976","#feb24c","#fd8d3c","#fc4e2a","#e31a1c","#bd0026","#800026")),

    "YlOrBr" -> Map(
      3 -> Array("#fff7bc","#fec44f","#d95f0e"),
      4 -> Array("#ffffd4","#fed98e","#fe9929","#cc4c02"),
      5 -> Array("#ffffd4","#fed98e","#fe9929","#d95f0e","#993404"),
      6 -> Array("#ffffd4","#fee391","#fec44f","#fe9929","#d95f0e","#993404"),
      7 -> Array("#ffffd4","#fee391","#fec44f","#fe9929","#ec7014","#cc4c02","#8c2d04"),
      8 -> Array("#ffffe5","#fff7bc","#fee391","#fec44f","#fe9929","#ec7014","#cc4c02","#8c2d04"),
      9 -> Array("#ffffe5","#fff7bc","#fee391","#fec44f","#fe9929","#ec7014","#cc4c02","#993404","#662506")),

    "Purples" -> Map(
      3 -> Array("#efedf5","#bcbddc","#756bb1"),
      4 -> Array("#f2f0f7","#cbc9e2","#9e9ac8","#6a51a3"),
      5 -> Array("#f2f0f7","#cbc9e2","#9e9ac8","#756bb1","#54278f"),
      6 -> Array("#f2f0f7","#dadaeb","#bcbddc","#9e9ac8","#756bb1","#54278f"),
      7 -> Array("#f2f0f7","#dadaeb","#bcbddc","#9e9ac8","#807dba","#6a51a3","#4a1486"),
      8 -> Array("#fcfbfd","#efedf5","#dadaeb","#bcbddc","#9e9ac8","#807dba","#6a51a3","#4a1486"),
      9 -> Array("#fcfbfd","#efedf5","#dadaeb","#bcbddc","#9e9ac8","#807dba","#6a51a3","#54278f","#3f007d")),

    "Blues" -> Map(
      3 -> Array("#deebf7","#9ecae1","#3182bd"),
      4 -> Array("#eff3ff","#bdd7e7","#6baed6","#2171b5"),
      5 -> Array("#eff3ff","#bdd7e7","#6baed6","#3182bd","#08519c"),
      6 -> Array("#eff3ff","#c6dbef","#9ecae1","#6baed6","#3182bd","#08519c"),
      7 -> Array("#eff3ff","#c6dbef","#9ecae1","#6baed6","#4292c6","#2171b5","#084594"),
      8 -> Array("#f7fbff","#deebf7","#c6dbef","#9ecae1","#6baed6","#4292c6","#2171b5","#084594"),
      9 -> Array("#f7fbff","#deebf7","#c6dbef","#9ecae1","#6baed6","#4292c6","#2171b5","#08519c","#08306b")),

    "Greens" -> Map(
      3 -> Array("#e5f5e0","#a1d99b","#31a354"),
      4 -> Array("#edf8e9","#bae4b3","#74c476","#238b45"),
      5 -> Array("#edf8e9","#bae4b3","#74c476","#31a354","#006d2c"),
      6 -> Array("#edf8e9","#c7e9c0","#a1d99b","#74c476","#31a354","#006d2c"),
      7 -> Array("#edf8e9","#c7e9c0","#a1d99b","#74c476","#41ab5d","#238b45","#005a32"),
      8 -> Array("#f7fcf5","#e5f5e0","#c7e9c0","#a1d99b","#74c476","#41ab5d","#238b45","#005a32"),
      9 -> Array("#f7fcf5","#e5f5e0","#c7e9c0","#a1d99b","#74c476","#41ab5d","#238b45","#006d2c","#00441b")),

    "Oranges" -> Map(
      3 -> Array("#fee6ce","#fdae6b","#e6550d"),
      4 -> Array("#feedde","#fdbe85","#fd8d3c","#d94701"),
      5 -> Array("#feedde","#fdbe85","#fd8d3c","#e6550d","#a63603"),
      6 -> Array("#feedde","#fdd0a2","#fdae6b","#fd8d3c","#e6550d","#a63603"),
      7 -> Array("#feedde","#fdd0a2","#fdae6b","#fd8d3c","#f16913","#d94801","#8c2d04"),
      8 -> Array("#fff5eb","#fee6ce","#fdd0a2","#fdae6b","#fd8d3c","#f16913","#d94801","#8c2d04"),
      9 -> Array("#fff5eb","#fee6ce","#fdd0a2","#fdae6b","#fd8d3c","#f16913","#d94801","#a63603","#7f2704")),

    "Reds" -> Map(
      3 -> Array("#fee0d2","#fc9272","#de2d26"),
      4 -> Array("#fee5d9","#fcae91","#fb6a4a","#cb181d"),
      5 -> Array("#fee5d9","#fcae91","#fb6a4a","#de2d26","#a50f15"),
      6 -> Array("#fee5d9","#fcbba1","#fc9272","#fb6a4a","#de2d26","#a50f15"),
      7 -> Array("#fee5d9","#fcbba1","#fc9272","#fb6a4a","#ef3b2c","#cb181d","#99000d"),
      8 -> Array("#fff5f0","#fee0d2","#fcbba1","#fc9272","#fb6a4a","#ef3b2c","#cb181d","#99000d"),
      9 -> Array("#fff5f0","#fee0d2","#fcbba1","#fc9272","#fb6a4a","#ef3b2c","#cb181d","#a50f15","#67000d")),

    "Greys" -> Map(
      3 -> Array("#f0f0f0","#bdbdbd","#636363"),
      4 -> Array("#f7f7f7","#cccccc","#969696","#525252"),
      5 -> Array("#f7f7f7","#cccccc","#969696","#636363","#252525"),
      6 -> Array("#f7f7f7","#d9d9d9","#bdbdbd","#969696","#636363","#252525"),
      7 -> Array("#f7f7f7","#d9d9d9","#bdbdbd","#969696","#737373","#525252","#252525"),
      8 -> Array("#ffffff","#f0f0f0","#d9d9d9","#bdbdbd","#969696","#737373","#525252","#252525"),
      9 -> Array("#ffffff","#f0f0f0","#d9d9d9","#bdbdbd","#969696","#737373","#525252","#252525","#000000")),

    "PuOr" -> Map(
      3  -> Array("#f1a340","#f7f7f7","#998ec3"),
      4  -> Array("#e66101","#fdb863","#b2abd2","#5e3c99"),
      5  -> Array("#e66101","#fdb863","#f7f7f7","#b2abd2","#5e3c99"),
      6  -> Array("#b35806","#f1a340","#fee0b6","#d8daeb","#998ec3","#542788"),
      7  -> Array("#b35806","#f1a340","#fee0b6","#f7f7f7","#d8daeb","#998ec3","#542788"),
      8  -> Array("#b35806","#e08214","#fdb863","#fee0b6","#d8daeb","#b2abd2","#8073ac","#542788"),
      9  -> Array("#b35806","#e08214","#fdb863","#fee0b6","#f7f7f7","#d8daeb","#b2abd2","#8073ac","#542788"),
      10 -> Array("#7f3b08","#b35806","#e08214","#fdb863","#fee0b6","#d8daeb","#b2abd2","#8073ac","#542788","#2d004b"),
      11 -> Array("#7f3b08","#b35806","#e08214","#fdb863","#fee0b6","#f7f7f7","#d8daeb","#b2abd2","#8073ac","#542788","#2d004b")),

    "BrBG" -> Map(
      3  -> Array("#d8b365","#f5f5f5","#5ab4ac"),
      4  -> Array("#a6611a","#dfc27d","#80cdc1","#018571"),
      5  -> Array("#a6611a","#dfc27d","#f5f5f5","#80cdc1","#018571"),
      6  -> Array("#8c510a","#d8b365","#f6e8c3","#c7eae5","#5ab4ac","#01665e"),
      7  -> Array("#8c510a","#d8b365","#f6e8c3","#f5f5f5","#c7eae5","#5ab4ac","#01665e"),
      8  -> Array("#8c510a","#bf812d","#dfc27d","#f6e8c3","#c7eae5","#80cdc1","#35978f","#01665e"),
      9  -> Array("#8c510a","#bf812d","#dfc27d","#f6e8c3","#f5f5f5","#c7eae5","#80cdc1","#35978f","#01665e"),
      10 -> Array("#543005","#8c510a","#bf812d","#dfc27d","#f6e8c3","#c7eae5","#80cdc1","#35978f","#01665e","#003c30"),
      11 -> Array("#543005","#8c510a","#bf812d","#dfc27d","#f6e8c3","#f5f5f5","#c7eae5","#80cdc1","#35978f","#01665e","#003c30")),

    "PRGn" -> Map(
      3  -> Array("#af8dc3","#f7f7f7","#7fbf7b"),
      4  -> Array("#7b3294","#c2a5cf","#a6dba0","#008837"),
      5  -> Array("#7b3294","#c2a5cf","#f7f7f7","#a6dba0","#008837"),
      6  -> Array("#762a83","#af8dc3","#e7d4e8","#d9f0d3","#7fbf7b","#1b7837"),
      7  -> Array("#762a83","#af8dc3","#e7d4e8","#f7f7f7","#d9f0d3","#7fbf7b","#1b7837"),
      8  -> Array("#762a83","#9970ab","#c2a5cf","#e7d4e8","#d9f0d3","#a6dba0","#5aae61","#1b7837"),
      9  -> Array("#762a83","#9970ab","#c2a5cf","#e7d4e8","#f7f7f7","#d9f0d3","#a6dba0","#5aae61","#1b7837"),
      10 -> Array("#40004b","#762a83","#9970ab","#c2a5cf","#e7d4e8","#d9f0d3","#a6dba0","#5aae61","#1b7837","#00441b"),
      11 -> Array("#40004b","#762a83","#9970ab","#c2a5cf","#e7d4e8","#f7f7f7","#d9f0d3","#a6dba0","#5aae61","#1b7837","#00441b")),

    "PiYG" -> Map(
      3  -> Array("#e9a3c9","#f7f7f7","#a1d76a"),
      4  -> Array("#d01c8b","#f1b6da","#b8e186","#4dac26"),
      5  -> Array("#d01c8b","#f1b6da","#f7f7f7","#b8e186","#4dac26"),
      6  -> Array("#c51b7d","#e9a3c9","#fde0ef","#e6f5d0","#a1d76a","#4d9221"),
      7  -> Array("#c51b7d","#e9a3c9","#fde0ef","#f7f7f7","#e6f5d0","#a1d76a","#4d9221"),
      8  -> Array("#c51b7d","#de77ae","#f1b6da","#fde0ef","#e6f5d0","#b8e186","#7fbc41","#4d9221"),
      9  -> Array("#c51b7d","#de77ae","#f1b6da","#fde0ef","#f7f7f7","#e6f5d0","#b8e186","#7fbc41","#4d9221"),
      10 -> Array("#8e0152","#c51b7d","#de77ae","#f1b6da","#fde0ef","#e6f5d0","#b8e186","#7fbc41","#4d9221","#276419"),
      11 -> Array("#8e0152","#c51b7d","#de77ae","#f1b6da","#fde0ef","#f7f7f7","#e6f5d0","#b8e186","#7fbc41","#4d9221","#276419")),

    "RdBu" -> Map(
      3  -> Array("#ef8a62","#f7f7f7","#67a9cf"),
      4  -> Array("#ca0020","#f4a582","#92c5de","#0571b0"),
      5  -> Array("#ca0020","#f4a582","#f7f7f7","#92c5de","#0571b0"),
      6  -> Array("#b2182b","#ef8a62","#fddbc7","#d1e5f0","#67a9cf","#2166ac"),
      7  -> Array("#b2182b","#ef8a62","#fddbc7","#f7f7f7","#d1e5f0","#67a9cf","#2166ac"),
      8  -> Array("#b2182b","#d6604d","#f4a582","#fddbc7","#d1e5f0","#92c5de","#4393c3","#2166ac"),
      9  -> Array("#b2182b","#d6604d","#f4a582","#fddbc7","#f7f7f7","#d1e5f0","#92c5de","#4393c3","#2166ac"),
      10 -> Array("#67001f","#b2182b","#d6604d","#f4a582","#fddbc7","#d1e5f0","#92c5de","#4393c3","#2166ac","#053061"),
      11 -> Array("#67001f","#b2182b","#d6604d","#f4a582","#fddbc7","#f7f7f7","#d1e5f0","#92c5de","#4393c3","#2166ac","#053061")),

    "RdGy" -> Map(
      3  -> Array("#ef8a62","#ffffff","#999999"),
      4  -> Array("#ca0020","#f4a582","#bababa","#404040"),
      5  -> Array("#ca0020","#f4a582","#ffffff","#bababa","#404040"),
      6  -> Array("#b2182b","#ef8a62","#fddbc7","#e0e0e0","#999999","#4d4d4d"),
      7  -> Array("#b2182b","#ef8a62","#fddbc7","#ffffff","#e0e0e0","#999999","#4d4d4d"),
      8  -> Array("#b2182b","#d6604d","#f4a582","#fddbc7","#e0e0e0","#bababa","#878787","#4d4d4d"),
      9  -> Array("#b2182b","#d6604d","#f4a582","#fddbc7","#ffffff","#e0e0e0","#bababa","#878787","#4d4d4d"),
      10 -> Array("#67001f","#b2182b","#d6604d","#f4a582","#fddbc7","#e0e0e0","#bababa","#878787","#4d4d4d","#1a1a1a"),
      11 -> Array("#67001f","#b2182b","#d6604d","#f4a582","#fddbc7","#ffffff","#e0e0e0","#bababa","#878787","#4d4d4d","#1a1a1a")),

    "RdYlBu" -> Map(
      3  -> Array("#fc8d59","#ffffbf","#91bfdb"),
      4  -> Array("#d7191c","#fdae61","#abd9e9","#2c7bb6"),
      5  -> Array("#d7191c","#fdae61","#ffffbf","#abd9e9","#2c7bb6"),
      6  -> Array("#d73027","#fc8d59","#fee090","#e0f3f8","#91bfdb","#4575b4"),
      7  -> Array("#d73027","#fc8d59","#fee090","#ffffbf","#e0f3f8","#91bfdb","#4575b4"),
      8  -> Array("#d73027","#f46d43","#fdae61","#fee090","#e0f3f8","#abd9e9","#74add1","#4575b4"),
      9  -> Array("#d73027","#f46d43","#fdae61","#fee090","#ffffbf","#e0f3f8","#abd9e9","#74add1","#4575b4"),
      10 -> Array("#a50026","#d73027","#f46d43","#fdae61","#fee090","#e0f3f8","#abd9e9","#74add1","#4575b4","#313695"),
      11 -> Array("#a50026","#d73027","#f46d43","#fdae61","#fee090","#ffffbf","#e0f3f8","#abd9e9","#74add1","#4575b4","#313695")),

    "Spectral" -> Map(
      3  -> Array("#fc8d59","#ffffbf","#99d594"),
      4  -> Array("#d7191c","#fdae61","#abdda4","#2b83ba"),
      5  -> Array("#d7191c","#fdae61","#ffffbf","#abdda4","#2b83ba"),
      6  -> Array("#d53e4f","#fc8d59","#fee08b","#e6f598","#99d594","#3288bd"),
      7  -> Array("#d53e4f","#fc8d59","#fee08b","#ffffbf","#e6f598","#99d594","#3288bd"),
      8  -> Array("#d53e4f","#f46d43","#fdae61","#fee08b","#e6f598","#abdda4","#66c2a5","#3288bd"),
      9  -> Array("#d53e4f","#f46d43","#fdae61","#fee08b","#ffffbf","#e6f598","#abdda4","#66c2a5","#3288bd"),
      10 -> Array("#9e0142","#d53e4f","#f46d43","#fdae61","#fee08b","#e6f598","#abdda4","#66c2a5","#3288bd","#5e4fa2"),
      11 -> Array("#9e0142","#d53e4f","#f46d43","#fdae61","#fee08b","#ffffbf","#e6f598","#abdda4","#66c2a5","#3288bd","#5e4fa2")),

    "RdYlGn" -> Map(
      3  -> Array("#fc8d59","#ffffbf","#91cf60"),
      4  -> Array("#d7191c","#fdae61","#a6d96a","#1a9641"),
      5  -> Array("#d7191c","#fdae61","#ffffbf","#a6d96a","#1a9641"),
      6  -> Array("#d73027","#fc8d59","#fee08b","#d9ef8b","#91cf60","#1a9850"),
      7  -> Array("#d73027","#fc8d59","#fee08b","#ffffbf","#d9ef8b","#91cf60","#1a9850"),
      8  -> Array("#d73027","#f46d43","#fdae61","#fee08b","#d9ef8b","#a6d96a","#66bd63","#1a9850"),
      9  -> Array("#d73027","#f46d43","#fdae61","#fee08b","#ffffbf","#d9ef8b","#a6d96a","#66bd63","#1a9850"),
      10 -> Array("#a50026","#d73027","#f46d43","#fdae61","#fee08b","#d9ef8b","#a6d96a","#66bd63","#1a9850","#006837"),
      11 -> Array("#a50026","#d73027","#f46d43","#fdae61","#fee08b","#ffffbf","#d9ef8b","#a6d96a","#66bd63","#1a9850","#006837")),

    "Accent" -> Map(
      3 -> Array("#7fc97f","#beaed4","#fdc086"),
      4 -> Array("#7fc97f","#beaed4","#fdc086","#ffff99"),
      5 -> Array("#7fc97f","#beaed4","#fdc086","#ffff99","#386cb0"),
      6 -> Array("#7fc97f","#beaed4","#fdc086","#ffff99","#386cb0","#f0027f"),
      7 -> Array("#7fc97f","#beaed4","#fdc086","#ffff99","#386cb0","#f0027f","#bf5b17"),
      8 -> Array("#7fc97f","#beaed4","#fdc086","#ffff99","#386cb0","#f0027f","#bf5b17","#666666")),

    "Dark2" -> Map(
      3 -> Array("#1b9e77","#d95f02","#7570b3"),
      4 -> Array("#1b9e77","#d95f02","#7570b3","#e7298a"),
      5 -> Array("#1b9e77","#d95f02","#7570b3","#e7298a","#66a61e"),
      6 -> Array("#1b9e77","#d95f02","#7570b3","#e7298a","#66a61e","#e6ab02"),
      7 -> Array("#1b9e77","#d95f02","#7570b3","#e7298a","#66a61e","#e6ab02","#a6761d"),
      8 -> Array("#1b9e77","#d95f02","#7570b3","#e7298a","#66a61e","#e6ab02","#a6761d","#666666")),

    "Paired" -> Map(
      3  -> Array("#a6cee3","#1f78b4","#b2df8a"),
      4  -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c"),
      5  -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99"),
      6  -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c"),
      7  -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c","#fdbf6f"),
      8  -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c","#fdbf6f","#ff7f00"),
      9  -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c","#fdbf6f","#ff7f00","#cab2d6"),
      10 -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c","#fdbf6f","#ff7f00","#cab2d6","#6a3d9a"),
      11 -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c","#fdbf6f","#ff7f00","#cab2d6","#6a3d9a","#ffff99"),
      12 -> Array("#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c","#fdbf6f","#ff7f00","#cab2d6","#6a3d9a","#ffff99","#b15928")),

    "Pastel1" -> Map(
      3 -> Array("#fbb4ae","#b3cde3","#ccebc5"),
      4 -> Array("#fbb4ae","#b3cde3","#ccebc5","#decbe4"),
      5 -> Array("#fbb4ae","#b3cde3","#ccebc5","#decbe4","#fed9a6"),
      6 -> Array("#fbb4ae","#b3cde3","#ccebc5","#decbe4","#fed9a6","#ffffcc"),
      7 -> Array("#fbb4ae","#b3cde3","#ccebc5","#decbe4","#fed9a6","#ffffcc","#e5d8bd"),
      8 -> Array("#fbb4ae","#b3cde3","#ccebc5","#decbe4","#fed9a6","#ffffcc","#e5d8bd","#fddaec"),
      9 -> Array("#fbb4ae","#b3cde3","#ccebc5","#decbe4","#fed9a6","#ffffcc","#e5d8bd","#fddaec","#f2f2f2")),

    "Pastel2" -> Map(
      3 -> Array("#b3e2cd","#fdcdac","#cbd5e8"),
      4 -> Array("#b3e2cd","#fdcdac","#cbd5e8","#f4cae4"),
      5 -> Array("#b3e2cd","#fdcdac","#cbd5e8","#f4cae4","#e6f5c9"),
      6 -> Array("#b3e2cd","#fdcdac","#cbd5e8","#f4cae4","#e6f5c9","#fff2ae"),
      7 -> Array("#b3e2cd","#fdcdac","#cbd5e8","#f4cae4","#e6f5c9","#fff2ae","#f1e2cc"),
      8 -> Array("#b3e2cd","#fdcdac","#cbd5e8","#f4cae4","#e6f5c9","#fff2ae","#f1e2cc","#cccccc")),

    "Set1" -> Map(
      3 -> Array("#e41a1c","#377eb8","#4daf4a"),
      4 -> Array("#e41a1c","#377eb8","#4daf4a","#984ea3"),
      5 -> Array("#e41a1c","#377eb8","#4daf4a","#984ea3","#ff7f00"),
      6 -> Array("#e41a1c","#377eb8","#4daf4a","#984ea3","#ff7f00","#ffff33"),
      7 -> Array("#e41a1c","#377eb8","#4daf4a","#984ea3","#ff7f00","#ffff33","#a65628"),
      8 -> Array("#e41a1c","#377eb8","#4daf4a","#984ea3","#ff7f00","#ffff33","#a65628","#f781bf"),
      9 -> Array("#e41a1c","#377eb8","#4daf4a","#984ea3","#ff7f00","#ffff33","#a65628","#f781bf","#999999")),

    "Set2" -> Map(
      3 -> Array("#66c2a5","#fc8d62","#8da0cb"),
      4 -> Array("#66c2a5","#fc8d62","#8da0cb","#e78ac3"),
      5 -> Array("#66c2a5","#fc8d62","#8da0cb","#e78ac3","#a6d854"),
      6 -> Array("#66c2a5","#fc8d62","#8da0cb","#e78ac3","#a6d854","#ffd92f"),
      7 -> Array("#66c2a5","#fc8d62","#8da0cb","#e78ac3","#a6d854","#ffd92f","#e5c494"),
      8 -> Array("#66c2a5","#fc8d62","#8da0cb","#e78ac3","#a6d854","#ffd92f","#e5c494","#b3b3b3")),

    "Set3" -> Map(
      3  -> Array("#8dd3c7","#ffffb3","#bebada"),
      4  -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072"),
      5  -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3"),
      6  -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462"),
      7  -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462","#b3de69"),
      8  -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462","#b3de69","#fccde5"),
      9  -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462","#b3de69","#fccde5","#d9d9d9"),
      10 -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462","#b3de69","#fccde5","#d9d9d9","#bc80bd"),
      11 -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462","#b3de69","#fccde5","#d9d9d9","#bc80bd","#ccebc5"),
      12 -> Array("#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462","#b3de69","#fccde5","#d9d9d9","#bc80bd","#ccebc5","#ffed6f")))

}