import scala.annotation.tailrec

//val min = 12.0
//val max = 22.0
//val pctLength = 0.2
//val pctOverlap = 0.5
//
//def markers(min: Double,
//            max: Double,
//            pctLength: Double,
//            pctOverlap: Double)
//           (x: Double): Seq[Double] = {
//
//  val length = (max - min) * pctLength
//
//  val increment = (1 - pctOverlap) * length
//
//  val start = x - (x % increment)
//
//  Stream
//    .continually(increment)
//    .scanLeft(start)(_ + _)
//    .takeWhile(_ < start + length)
//    .toList
//}
//
//
//val m_02_00 = markers(12.0, 22.0, 0.2, 0.0) _
//val m_02_05 = markers(12.0, 22.0, 0.2, 0.5) _
//
//val pigs = List(12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5)
//
//pigs.map(p => m_02_00(p))
//pigs.map(p => m_02_05(p))


//def markers2(min: BigDecimal,
//             max: BigDecimal,
//             pctLength: BigDecimal,
//             pctOverlap: BigDecimal)
//            (value: Double): Seq[BigDecimal] = {
//
//  val length = (max - min) * pctLength
//
//  val increment = (1 - pctOverlap) * length
//
//  val x = BigDecimal(value)
//
//  val diff = (x - min) % increment
//
//  val offset = length quot increment
//
//  val start = (x - diff) - (offset * increment)
//
//  Stream
//    .continually(increment)
//    .scanLeft(start)(_ + _)
//    .filter(v => v >= min && v <= max)
//    .toList
//}
//
//val m_02_07 = markers2(12.0, 22.0, 0.2, 0.7) _
//
//m_02_07(12.0)
// m_02_07(14)
// m_02_07(16.9)

Vector(1) :+ 2

def toCoordinates[A](coveringValues: List[List[A]]) = {

  @tailrec
  def recur(acc: List[Vector[A]],
            values: List[List[A]]): List[Vector[A]] = values match {
    case Nil => acc
    case x :: xs => recur(x.flatMap(v => acc.map(combos => combos :+ v)), xs)
  }

  recur(List(Vector[A]()), coveringValues)
}

toCoordinates(List(List("1", "2"), List("a", "b")))

toCoordinates(List(List("1", "2", "3"), List("a", "b", "c")))

toCoordinates(List(List("1", "2"), List("a", "b"), List("X", "Y")))

