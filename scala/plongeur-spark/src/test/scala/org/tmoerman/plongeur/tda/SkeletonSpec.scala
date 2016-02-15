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

    val clean = result.map(_.map(s => StringUtils.replaceChars(s.toString, "-", "_")))

    val graph =
      Seq(
        "graph X {",
        clean.flatten.toSet.mkString("\n"),
        clean.map(_.toArray match { case Array(x, y) => s"$x -- $y" }).mkString("\n"),
        "}").mkString("\n")

    println(graph)
  }



}
