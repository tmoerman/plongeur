package org.tmoerman.plongeur.tda

import org.apache.commons.lang.StringUtils
import org.apache.spark.mllib.regression.LabeledPoint
import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Model.{Filter, Lens}
import org.tmoerman.plongeur.test.{TestResources, SparkContextSpec}

/**
  * @author Thomas Moerman
  */
class SkeletonSpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {
  import Inspections._

  behavior of "the skeleton"

  it should "work with specified boundaries" in {

    val boundaries = Array((0.0, 12.0), (0.0, 12.0))

    val lens = Lens(Filter((l: LabeledPoint) => l.features(0), 1.0, 0.5),
                    Filter((l: LabeledPoint) => l.features(1), 1.0, 0.5))

    val result =
      Skeleton.execute(
        lens = lens,
        data = test2DLabeledPointsRDD,
        boundaries = Some(boundaries))

    println(result.dotGraph("test2D"))
  }

  it should "work with calculated boundaries" in {

    val lens = Lens(Filter((l: LabeledPoint) => l.features(0), 1.0, 0.5),
                    Filter((l: LabeledPoint) => l.features(1), 1.0, 0.5))

    val result =
      Skeleton.execute(
        lens = lens,
        data = test2DLabeledPointsRDD)

    val intro = result.clusterPoints

    println(result.dotGraph("test2Dc"))
  }

  it should "recover the 100 entries circle topology" in {

    val lens = Lens(Filter((l: LabeledPoint) => l.features(0), 0.5, 0.5))

    val result =
      Skeleton.execute(
        lens = lens,
        data = circle1kRDD)

    println(result.dotGraph("Circle1K"))
  }


}
