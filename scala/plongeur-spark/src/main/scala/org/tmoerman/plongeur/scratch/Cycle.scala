package org.tmoerman.plongeur.scratch

/**
  * Aide-mémoire for the cycle architecture.
  *
  * @author Thomas Moerman
  */
object Cycle {

  sealed trait Key
  case object DOM extends Key
  case object CONSOLE extends Key

  trait Effect
  trait ReadEffect extends Effect
  trait WriteEffect extends Effect

  trait Observable[E <: Effect] {
    def pipeTo(other: Observable[E]): Unit = {}
  }

  type Source = Observable[ReadEffect]
  type Sink   = Observable[WriteEffect]

  type Main = Map[Key, Source] => Map[Key, Sink]

  type Driver = Sink => Option[Source]

  def main(sources: Map[Key, Source]): Map[Key, Sink] = {
    sources.mapValues(_ => new Sink {})
  }

  def run(mainFn: Main, drivers: Map[Key, Driver]): Unit = {
    val proxySources = drivers.mapValues(_ => new Source {})

    val sinks = mainFn.apply(proxySources)

    drivers
      .flatMap{ case (key, driver) => driver.apply(sinks(key)).map(source => (key, source)) }
      .foreach{ case (key, source) => source.pipeTo(proxySources(key)) }
  }

  def DOMDriver(sink: Sink): Option[Source] = {
    Option(new Source {})
  }

  def consoleDriver(sink: Sink): Option[Source] = {
    None
  }

  val drivers: Map[Key, Driver] = Map(
    DOM     -> DOMDriver     _,
    CONSOLE -> consoleDriver _)

  run(main, drivers)

}