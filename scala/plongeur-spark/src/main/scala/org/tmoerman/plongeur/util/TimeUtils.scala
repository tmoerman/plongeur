package org.tmoerman.plongeur.util

import java.lang.System.nanoTime
import java.util.concurrent.TimeUnit.NANOSECONDS

import scala.concurrent.duration.Duration

/**
  * @author Thomas Moerman
  */
object TimeUtils {

  def time[R](block: => R): (R, Duration) = {
    val start  = nanoTime
    val result = block
    val done   = nanoTime

    (result, Duration(done - start, NANOSECONDS))
  }

}