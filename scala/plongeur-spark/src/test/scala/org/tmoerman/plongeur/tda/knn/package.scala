package org.tmoerman.plongeur.tda

import java.lang.Math._

import org.apache.spark.mllib.linalg.SparseMatrix
import org.apache.spark.mllib.linalg.Vectors._
import org.scalatest.Matchers
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.KNN._
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
package object knn extends Matchers {

  /** 7
    * 6              .7 .8
    * 5              .5 .6
    * 4           .4
    * 3     .2 .3
    * 2     .0 .1
    * 1
    * 0  1  2  3  4  5  6  7
    */
  val points = Seq(
    dp(0, dense(2.0, 2.0)),
    dp(1, dense(2.0, 3.0)),
    dp(2, dense(3.0, 2.0)),
    dp(3, dense(3.0, 3.0)),
    dp(4, dense(4.0, 4.0)),
    dp(5, dense(5.0, 5.0)),
    dp(6, dense(5.0, 6.0)),
    dp(7, dense(6.0, 5.0)),
    dp(8, dense(6.0, 6.0)))

  val k2ExpectedFreqs = Map(1.0 -> 16, sqrt(2) -> 2)

  def assertDistanceFrequencies(acc: ACCLike,
                                expectedFreqs: Map[Distance, Count] = k2ExpectedFreqs): Unit =
    acc
      .flatMap(_._2.map(_._2))
      .frequencies shouldBe expectedFreqs

  def assertDistanceFrequenciesM(m: SparseMatrix,
                                 expectedFreqs: Map[Distance, Count] = k2ExpectedFreqs): Unit =
    m
      .values
      .toIterable
      .filter(_ > 0.0)
      .frequencies shouldBe expectedFreqs

}