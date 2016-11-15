package org.tmoerman.cases.mixture

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}

/**
  * @author Thomas Moerman
  */
class MixturesSpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  val mixture = wd + "mixture.n1000.d20.cat2.csv"

  "reading mixtures" should "work" in {

    val rdd = readMixture(mixture)(sc)

    println(rdd.collect.last)

  }

}