//addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
resolvers += "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven/"

addSbtPlugin("org.spark-packages" % "sbt-spark-package" % "0.2.5")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")