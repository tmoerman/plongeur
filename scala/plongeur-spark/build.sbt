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

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath + "/.m2/repository")))

libraryDependencies ++= Seq(

  "org.apache.spark"       % "spark-core_2.10"   % sparkVersion % "provided",
  "org.apache.spark"       % "spark-mllib_2.10"  % sparkVersion exclude("com.chuusai", "*"),
  "com.esotericsoftware"   % "kryo"              % "3.0.3",

  "com.github.haifengl"    % "smile-core"        % "1.0.4",
  "com.github.haifengl"    % "smile-plot"        % "1.0.4",
  "org.scalanlp"           %% "breeze-natives"   % "0.12"       exclude("com.chuusai", "*"),

  "io.reactivex"           %% "rxscala"          % "0.26.1",
  "org.scalaz"             %% "scalaz-core"      % "7.2.0",
  "com.chuusai"            %% "shapeless"        % "2.3.0",

  "org.scalatest"          %% "scalatest"        % "2.2.4"  % "test",

  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

)