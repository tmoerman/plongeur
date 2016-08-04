package org.tmoerman.plongeur.tda

import org.tmoerman.plongeur.tda.Model.{Cluster, DataPoint, TDAContext}
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
object Colouring extends Serializable {

  trait ColouringFunction[T] extends Serializable

  trait ColouringPredicate extends ColouringFunction[Boolean]

  abstract case class BroadcastPredicate(val key: String) extends (Any => DataPoint => Boolean) with ColouringPredicate

  abstract case class AttributePredicate(val key: String) extends (Any => Boolean) with ColouringPredicate

  trait Binner extends (Cluster => Iterable[Int]) with Serializable

  /**
    * @return Returns a binner that maps every cluster to no bins.
    */
  def void() = new Binner {
    override def apply(v1: Cluster): Iterable[Int] = None
  }

  /**
    * @param bin
    * @return Returns a binner that maps every cluster to a specified bin.
    */
  def trivial(bin: Int) = new Binner {
    override def apply(cluster: Cluster): Iterable[Int] = Some(bin)
  }

  def dataPointPredicate(colouringPredicate: ColouringPredicate, ctx: TDAContext) = colouringPredicate match {
    case pred@AttributePredicate(key) =>
      (dp: DataPoint) => dp.meta.flatMap(_.get(key)).map(pred(_)).getOrElse(false)

    case pred@BroadcastPredicate(key) =>
      ctx.broadcasts.get(key).map(bc => pred.apply(bc)).getOrElse((_: DataPoint) => false)
  }

  def pctToBin(nrBins: Int, pct: Double) = (pct / (1d / nrBins)).toInt
  
  /**
    * @param nrBinsResolution
    * @param colouringPredicate
    * @return Returns a Binner that maps a cluster to a bin according to a predefined nr of bins and the amount of
    *         DataPoint instances that satisfy the specified predicate.
    */
  def localPercentage(nrBinsResolution: Int, colouringPredicate: ColouringPredicate, ctx: TDAContext) = new Binner {

    override def apply(cluster: Cluster): Iterable[Int] = {
      val predicate = dataPointPredicate(colouringPredicate, ctx)

      val freqs = cluster.dataPoints.map(predicate).frequencies

      val pctTrue = freqs(true).toDouble / freqs(false)

      val bin = pctToBin(nrBinsResolution, pctTrue)

      Seq(bin)
    }

  }

  /**
    * @param colouringPredicate
    * @param ctx
    * @return Returns a Binner that maps a cluster to the first (0) bin if it has DataPoint instances that satisfy
    *         the specified predicate.
    */
  def localOccurrence(colouringPredicate: ColouringPredicate, ctx: TDAContext) = new Binner {

    override def apply(cluster: Cluster): Iterable[Int] = {
      val dataPointPredicate = dataPointPredicate(colouringPredicate, ctx)

      val exists = cluster.dataPoints.exists(dataPointPredicate)

      if (exists) Seq(0) else Seq()
    }

  }

}

case class Colouring(palette: Seq[String], )