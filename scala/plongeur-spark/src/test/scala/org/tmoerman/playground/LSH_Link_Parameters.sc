import java.lang.Math._


val f = 10
val C = 0.0035
val r = C / f  // both are fully determined by f
val d = 25
val k = (d*f / 2) * sqrt(d)
val P_miss = 0.01
val A = 1.2
val l = log(P_miss) / log(1. - pow(1. - 1. / (d*f), k))

val nrIterations =
  Stream
    .from(1)
    .scan(r){ case (r: Double, i: Int) => r * A }
    .takeWhile{ case x:Double => x <= C }
    .size
