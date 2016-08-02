package org.tmoerman.plongeur.ui

import declarativewidgets.Channel
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model.{Filter, TDALens, TDAParams}
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
        Filter("PCA" :: 0 :: HNil, 10, 0.5),
        Filter("PCA" :: 1 :: HNil, 10, 0.5)),
      scaleSelection = histogram(10))

  behavior of "controls"

  it should "return css correctly" in {
    println(Controls.controlsCSS)
  }

  val ch = Channel(null, "meh")

  val in$ = PublishSubject[TDAParams]

  it should "compile with a channel" in {
    Try(pca0.makeControls(ch, in$))
  }

}