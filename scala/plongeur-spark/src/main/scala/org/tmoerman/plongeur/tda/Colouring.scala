package org.tmoerman.plongeur.tda

import org.tmoerman.plongeur.tda.Model.{Cluster, DataPoint, TDAContext}
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
object Colouring extends Serializable {

  trait Selector[T] extends Serializable

  abstract case class BroadcastSelector[T](val key: String) extends (Any => DataPoint => T) with Selector[T]

  abstract case class AttributeSelector[T](val key: String) extends (Any => T) with Selector[T]

  trait Binner extends (Cluster => Iterable[Int]) with Serializable

  def dataPointPredicate(selector: Selector[Boolean], ctx: TDAContext) = (dp: DataPoint) => selector match {
    case pred@AttributeSelector(key) => dp.meta.flatMap(_.get(key)).exists(pred)
    case pred@BroadcastSelector(key) => ctx.broadcasts.get(key).exists(bc => pred(bc)(dp))
  }

  def dataPointCategorizer[T](selector: Selector[T], ctx: TDAContext) = (dp: DataPoint) => selector match {
    case fn@AttributeSelector(key) => dp.meta.flatMap(_.get(key)).map(fn).get
    case fn@BroadcastSelector(key) => ctx.broadcasts.get(key).map(bc => fn(bc)(dp)).get
  }

  /**
    * @param nrBinsResolution
    * @param selectorPredicate
    * @return Returns a Binner that maps a cluster to a bin according to a predefined nr of bins and the amount of
    *         DataPoint instances that satisfy the specified predicate.
    */
  case class LocalPercentage(nrBinsResolution: Int, selectorPredicate: Selector[Boolean], ctx: TDAContext) extends Binner {

    override def apply(cluster: Cluster) = {
      val predicate = dataPointPredicate(selectorPredicate, ctx)

      val freqs = cluster.dataPoints.toStream.map(predicate).frequencies

      val pctTrue = freqs(true).toDouble / freqs(false)

      val bin = pctToBin(nrBinsResolution, pctTrue)

      Seq(bin)
    }

  }

  def pctToBin(nrBins: Int, pct: Double) = (pct / (1d / nrBins)).toInt

  /**
    * A Binner that maps a cluster to the first (0) bin if it has DataPoint instances that satisfy
    * the specified predicate.
    *
    * @param selectorPredicate
    * @param ctx
    * @return Returns
    */
  case class LocalOccurrence(selectorPredicate: Selector[Boolean], ctx: TDAContext) extends Binner {

    override def apply(cluster: Cluster) = {
      val predicate = dataPointPredicate(selectorPredicate, ctx)

      val exists = cluster.dataPoints.exists(predicate)

      if (exists) Seq(0) else Seq()
    }

  }

  /**
    * A Binner that maps a cluster to the bin corresponding to the category of which the cluster has
    * the most DataPoint instances.
    *
    * @param categorySelector
    * @param binMapping
    * @param ctx
    * @tparam T
    */
  case class LocalMaxFreq[T](categorySelector: Selector[T], binMapping: Map[T, Int], ctx: TDAContext) extends Binner {

    override def apply(cluster: Cluster) = {
      val selector = dataPointCategorizer(categorySelector, ctx)

      val freqs = cluster.dataPoints.toStream.map(selector).frequencies

      val winner = freqs.maxBy(_._2)

      binMapping.get(winner._1)
    }

  }

  /**
    * A binner that maps every cluster to a specified bin.
    *
    * @param bin
    */
  case class Constantly(bin: Int = 0) extends Binner {

    override def apply(cluster: Cluster) = Some(bin)

  }

  /**
    * A Binner that maps every cluster to no bins.
    */
  case class Nop() extends Binner {

    override def apply(cluster: Cluster) = None

  }

}