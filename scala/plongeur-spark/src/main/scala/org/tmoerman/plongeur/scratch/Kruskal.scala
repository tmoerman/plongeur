package org.tmoerman.plongeur.scratch

/**
  * Created by tmo on 18/03/16.
  */
object Kruskal {

  case class Edge[A](a1: A, a2: A, distance: Double)

  type LE[A] = List[Edge[A]]
  type LS[A] = List[Set[A]]
  type OS[A] = Option[Set[A]]

  def kruskal[A](list: LE[A]) =
    list
      .sortBy(_.distance)
      .foldLeft( (Nil: LE[A], Nil: LS[A]) )(mst)
      ._1

  def mst[A](acc: (LE[A], LS[A]), edge: Edge[A]) = (acc, edge) match {

    case ((edges, sets), Edge(p, q, _)) => // destructure the accumulator and the edge

      val extract =
        sets
          .foldRight((Nil: LS[A], None: OS[A], None: OS[A])) {
            (set: Set[A], triple: (LS[A], OS[A], OS[A])) => triple match {
              case (list, setp, setq) =>
                val sp = set.contains(p)
                val sq = set.contains(q)

                (if (sp || sq) list else set :: list,
                 if (sp) Some(set) else setp,
                 if (sq) Some(set) else setq) }}

      extract match {
        case (rest, None,     None    ) => (edge :: edges, Set(p, q) :: rest)
        case (rest, Some(ps), None    ) => (edge :: edges,    ps + q :: rest)
        case (rest, None,     Some(qs)) => (edge :: edges,    qs + p :: rest)
        case (rest, Some(ps), Some(qs)) => if (ps == qs) (edges, sets) // circle
        else (edge :: edges, ps ++ qs :: rest)
      }

  }

}
