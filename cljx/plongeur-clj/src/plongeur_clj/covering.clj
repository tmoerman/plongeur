(ns plongeur-clj.covering)

;; scala> Stream.continually(5).scanLeft(1)(_ + _).take(10).force
;; res2: scala.collection.immutable.Stream[Int] = Stream(1, 6, 11, 16, 21, 26, 31, 36, 41, 46)

(defn uniform-covering-intervals
  [boundary-min boundary-max length-pct overlap-pct x]
  (let [interval-length (-> (- boundary-max boundary-min) (* length-pct))
        increment       (-> (- 1 overlap-pct) (* interval-length))
        diff            (-> (- x boundary-min) (mod increment))
        base            (- x diff)
        q       (quot interval-length increment)
        r       (mod  interval-length increment)
        factor  (if (= r 0) (dec q) q)
        start   (-> (- base increment) (* factor))
        end     (-> (+ base increment) )]

    (->> (repeat increment)
         (reductions + start)
         (take-while #(< %1 end)))
    ))
