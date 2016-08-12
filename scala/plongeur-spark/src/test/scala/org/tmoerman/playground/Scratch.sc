import org.tmoerman.plongeur.tda.Model.Cluster

// sorting Hi->lo with - in front of the selector function
Seq(("a", 5), ("b", 2)).sortBy(- _._2)