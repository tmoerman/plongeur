package org.tmoerman.plongeur.meta

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Model.DataPoint
import rx.lang.scala.Observable

/**
  * @author Thomas Moerman
  */
object MetaMachine {

  trait MachineParams

  case class DataProvider(val dataFn: () => RDD[DataPoint]) {
    lazy val dataPoints = dataFn.apply
    lazy val N = dataPoints.count
  }

  case class MetaConfig(dataProvider: DataProvider,
                        machineParams: Set[MachineParams])

  type Memo = Map[_ <: Serializable, _ <: Serializable]

  def create() = ???

  def run(cfg$: Observable[MetaConfig])
         (implicit sc: SparkContext) = {

    val data$ = cfg$.map(_.dataProvider)

    val machineParams$ = cfg$.map(_.machineParams)

    // val cfg_memo_$: Observable[(MetaConfig, Memo)] = cfg$.map(cfg => (cfg, Map()))

    // cfg_memo_$

    ???
  }

}