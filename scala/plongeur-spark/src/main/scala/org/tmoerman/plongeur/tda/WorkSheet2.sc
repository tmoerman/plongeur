val max = BigDecimal(22)
val min = BigDecimal(12)
val pctLength  = BigDecimal(0.2)
val pctOverlap = BigDecimal(0.7)

val length = (max - min) * pctLength

val increment = (1 - pctOverlap) * length

val offset = length quot increment

val x = BigDecimal(21.9)

val diff = (x - min) % increment

val start = x - diff - offset * increment

Stream
  .continually(increment)
  .scanLeft(start)(_ + _)
  .takeWhile(_ < start + length)
  .toList