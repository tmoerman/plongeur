package org.tmoerman.plongeur.tda.knn

import org.tmoerman.plongeur.tda.Distances.{DistanceFunction, EuclideanDistance}
import org.tmoerman.plongeur.tda.Model.DataPoint
import org.tmoerman.plongeur.tda.knn.FastKNN._
import org.tmoerman.plongeur.tda.knn.KNN._

/**
  * @author Thomas Moerman
  */
class FastKNNSpec extends KNNSpec {

  "init and concat" should "yield correct frequencies" in {
    implicit val d: DistanceFunction = EuclideanDistance

    val acc = toAcc(points)

    assertDistanceFrequencies(acc)
  }

  "init, concat and union" should "yield correct frequencies" in {
    implicit val d: DistanceFunction = EuclideanDistance

    val (a, b) = points.splitAt(4)
    val acc = union(toAcc(a), toAcc(b))

    assertDistanceFrequencies(acc)
  }

  "Breeze sparse matrix" should "yield correct frequencies" in {
    implicit val d: DistanceFunction = EuclideanDistance

    val acc = toAcc(points)
    val m = toSparseMatrix(points.size, acc)

    assertDistanceFrequencies(m)
  }

  def toAcc(points: Seq[DataPoint])(implicit k: Int = 2, d: DistanceFunction): ACC =
    (points: @unchecked) match {
      case x :: xs => xs.foldLeft(init(x))(concat)
    }

}