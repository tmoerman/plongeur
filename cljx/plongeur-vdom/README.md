# plongeur-vdom

FIXME: Write a one-line description of your library/project.

## Overview

FIXME: Write a paragraph about the library/project and highlight its goals.



## Cycle architecture


```JavaScript
function run(main, drivers) {
  let sinkProxies = makeSinkProxies(drivers)
  let sources = callDrivers(drivers, sinkProxies)
  let sinks = main(sources)
  let subscription = replicateMany(sinks, sinkProxies).subscribe()
  let sinksWithDispose = attachDisposeToSinks(sinks, subscription)
  let sourcesWithDispose = attachDisposeToSources(sources)

  return {sources: sourcesWithDispose, sinks: sinksWithDispose}
}
```


```Scala
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
```


## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
