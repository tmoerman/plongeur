package org.tmoerman.lab

import org.scalatest.{Matchers, FlatSpec}

/**
  * @author Thomas Moerman
  */
class XmlLab extends FlatSpec with Matchers {

  behavior of "generating html with xml literals"

  it should "produce an html string" in {

    val html = <ul>{List(1, 2, 3).map(i => <li>{i}</li>)}</ul>

    html.toString shouldBe "<ul><li>1</li><li>2</li><li>3</li></ul>"

  }

}