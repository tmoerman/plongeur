import sbt.Keys._

organization := "org.tmoerman"

name := "plongeur-spark"

description := "Plongeur Spark algorithms module"

scalaVersion := "2.10.4"

val localM2 = Path.userHome.absolutePath + "/.m2/repository"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),

  Resolver.mavenLocal
)

val sparkVersion  = "1.6.1"

publishTo := Some(Resolver.mavenLocal)

libraryDependencies ++= Seq(

  "org.apache.spark"       % "spark-core_2.10"   % sparkVersion % "provided",
  "org.apache.spark"       % "spark-mllib_2.10"  % sparkVersion exclude("com.chuusai", "*"),
  "org.scalanlp"           %% "breeze-natives"   % "0.11.2"     exclude("com.chuusai", "*"),
  "com.esotericsoftware"   % "kryo"              % "4.0.0",

  "com.github.haifengl"    % "smile-core"        % "1.2.0",

  "io.reactivex"           %% "rxscala"          % "0.26.1",
  "org.scalaz"             % "scalaz-core_2.10"  % "7.2.0",

  "com.github.karlhigley"  %% "spark-neighbors"  % "0.3.6-FORK" exclude("org.apache.spark", "*") exclude("org.scalanlp", "*"),

  "com.softwaremill.quicklens" %% "quicklens"    % "1.4.4",

  "org.scalatest"          %% "scalatest"        % "2.2.4"  % "test",

  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

)