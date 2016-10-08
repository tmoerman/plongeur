import java.lang.Math._

// Cfr. paper Koga

val f = 10
val C = 0.0035
val r = C / f  // both are fully determined by f
val d = 750
val k = (d*f / 2) * sqrt(d)
val P_miss = 0.5
val A = 1.2
val l = log(P_miss) / log(1. - pow(1. - 1. / (d*f), k))

val nrIterations =
  Stream
    .from(1)
    .scan(r){ case (r: Double, i: Int) => r * A }
    .takeWhile{ case x:Double => x <= C }
    .size


// Cfr. paper Indyk p-stable
//
// Apparently no dependency on the dimensionality of the data?

val p1 = 0.7
val p2 = 0.3
val n = 100000.
val _k = log(n) / log(1. / p2)
val rho = log(1. / p1) / log(1. / p2)
val L = pow(n, rho)