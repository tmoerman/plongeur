package org.tmoerman.plongeur.ui

import declarativewidgets.Channel
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model.{PrincipalComponent, Filter, TDALens, TDAParams}
import org.tmoerman.plongeur.tda.cluster.Scale._
import rx.lang.scala.subjects.PublishSubject
import shapeless.HNil

import Controls._

import scala.util.Try

/**
  * @author Thomas Moerman
  */
class ControlsSpec extends FlatSpec with Matchers {

  val pca0 =
    TDAParams(
      lens = TDALens(
        Filter(PrincipalComponent(0), 10, 0.5),
        Filter(PrincipalComponent(1), 10, 0.5)),
      scaleSelection = histogram(10))

  behavior of "controls"

  it should "return css correctly" in {
    println(Controls.controlsCSS)
  }

  val in$ = PublishSubject[TDAParams]

  it should "compile with a channel" ignore {
//    val ch = Channel(null, "meh")
//
//    Try(pca0.makeControls(ch, in$))
  }

}