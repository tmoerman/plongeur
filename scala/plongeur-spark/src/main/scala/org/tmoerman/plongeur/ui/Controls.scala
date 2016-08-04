package org.tmoerman.plongeur.ui

import org.tmoerman.plongeur.tda.Model.TDAParams
import org.tmoerman.plongeur.tda.Model.TDAParams.{setCollapseDuplicateClusters, setFilterNrBins, setFilterOverlap, setHistogramScaleSelectionNrBins}
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.{Observable, Subject, Subscription}

/**
  * @author Thomas Moerman
  */
object Controls extends Serializable {

  implicit def pimpTDAParams(params: TDAParams): HtmlControls = new HtmlControls(params)

  private val classes =
    """
      |table.clean th {
      |    border-style: hidden;
      |    white-space: nowrap;
      |}
      |table.clean td {
      |    border-style: hidden;
      |}
      |tr.title {
      |    text-align: center;
      |    background-color: beige;
      |}
      |td.wide {
      |    width: 500px;
      |}
      |td.wide paper-slider {
      |   width: 100%;
      |}
      |th.code {
      |   font-family: courier new,monospace;
      |}
    """.stripMargin

  val controlsCSS = <style>{classes}</style>

}

class HtmlControls(val tdaParams: TDAParams) extends Serializable {

  val LIMIT = 100

  type HandlerDuck[T] = (scala.Option[T], T) => Unit

  type ChannelDuck = {
    val chan: String
    def watch(variable: String, callback: HandlerDuck[_]): Unit
    def set(key: String, value : Any, limit: Int): Unit
  }

  private def filterVar(idx: Int, variable: String) = s"filter-$idx-$variable"

  private def nrBinsVarName (idx: Int) = filterVar(idx, "nrBins")
  private def overlapVarName(idx: Int) = filterVar(idx, "overlap")

  private val scaleBinsVarName = "scaleBins"
  private val collapseVarName = "collapse"

  def htmlControls(channel: ChannelDuck) = {
    import tdaParams._

    <template is="urth-core-bind" channel={channel.chan}>
      <table class="clean"> {
        lens.filters.zipWithIndex.map{ case (f, idx) => {

          val nrBinsVar  = nrBinsVarName(idx)
          val overlapVar = overlapVarName(idx)

          <tr class="title">
            <th>Filter</th>
            <th colspan="2" class="code">{f.spec}</th>
          </tr>
          <tr>
            <th>Nr of cover bins</th>
            <td class="wide"><paper-slider min="0" max="100" step="1" value={s"{{$nrBinsVar}}"}/></td>
            <td>[[{nrBinsVar}]]</td>
          </tr>
          <tr>
            <th>Cover overlap</th>
            <td class="wide"><paper-slider min="0" max="75" step="1" value={s"{{$overlapVar}}"}/></td>
            <td>[[{overlapVar}]]%</td>
          </tr>
        }}}
        <tr class="title">
          <th></th>
          <th colspan="2" class="code">General</th>
        </tr>
        <tr>
          <th>Nr of scale bins</th>
          <td class="wide"><paper-slider min="0" max="150" step="1" value="{{scaleBins}}"/></td>
          <td>[[scaleBins]]</td>
        </tr>
        <tr>
          <th>Collapse duplicates</th>
          <td colspan="2"><paper-toggle-button checked="{{collapse}}"/></td>
        </tr>
      </table>
    </template>
  }

  // TODO idea: cache SubRefs somehow?

  type Update$ = Observable[TDAParams => TDAParams]

  def makeSource[T](name: String, channel: ChannelDuck): Subject[T] = {

    val subject = PublishSubject[T]

    channel.watch(name, (_: Any, v: Any) => subject.onNext(v.asInstanceOf[T]))

    subject
  }

  def scaleBins$(channel: ChannelDuck) =
    makeSource[Int](scaleBinsVarName, channel).map(v => setHistogramScaleSelectionNrBins(v))

  def collapse$(channel: ChannelDuck) =
    makeSource[Boolean](collapseVarName, channel).map(v => setCollapseDuplicateClusters(v))

  def nrBins$(idx: Int, channel: ChannelDuck) =
    makeSource[Int](nrBinsVarName(idx), channel).map(v => setFilterNrBins(idx, v))

  def overlap$(idx: Int, channel: ChannelDuck) =
    makeSource[Int](overlapVarName(idx), channel).map(v => setFilterOverlap(idx, BigDecimal(v) / 100))

  def params$(channel: ChannelDuck): Observable[TDAParams] =
    (scaleBins$(channel) ::
     collapse$(channel) ::
     tdaParams.lens.filters.zipWithIndex.flatMap{ case (_, idx) =>
       nrBins$(idx, channel) ::
       overlap$(idx, channel) ::
       Nil })
    .reduce(_ merge _)
    .scan(tdaParams){ (params, fn) => fn.apply(params) }

  def initializeValues(channel: ChannelDuck): Unit = {
    channel.set(scaleBinsVarName, tdaParams.scaleSelection.resolution, LIMIT)
    channel.set(collapseVarName, tdaParams.collapseDuplicateClusters, LIMIT)

    tdaParams
      .lens.filters.zipWithIndex.foreach{ case (filter, idx) => {
        channel.set(nrBinsVarName(idx), filter.nrBins, LIMIT)
        channel.set(overlapVarName(idx), (filter.overlap * 100).toInt, LIMIT)
      }}
  }

  def makeControls(channel: ChannelDuck, in$: Subject[TDAParams]): (Subscription, String) = {
    initializeValues(channel)

    val subscription = params$(channel).subscribe(in$)

    val html = htmlControls(channel)

    (subscription, html.toString)
  }

}