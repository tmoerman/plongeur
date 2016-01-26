import sbt.Keys._

organization := "org.tmoerman"

name := "plongeur-spark"

description := "Plongeur Spark algorithms module"

scalaVersion := "2.10.6"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

val sparkVersion  = "1.6.0"

libraryDependencies ++= Seq(

  "org.apache.spark"      % "spark-core_2.10"   % sparkVersion,
  "org.apache.spark"      % "spark-mllib_2.10"  % sparkVersion,

  "nz.ac.waikato.cms.weka" % "weka-stable"      % "3.6.13",

  "com.esotericsoftware"  % "kryo"              % "3.0.3",

  "org.scalatest"         %% "scalatest"        % "2.2.4"  % "test"

  // "org.scalaz"         %% "scalaz-core"               % "7.2.0",
  // "com.chuusai"        %% "shapeless"                 % "2.2.5",
  // compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)

)
