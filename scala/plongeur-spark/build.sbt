import sbt.Keys._

organization := "org.tmoerman"

name := "plongeur-spark"

description := "Plongeur Spark algorithms module"

scalaVersion := "2.11.8"

sparkVersion := "2.0.2"

sparkComponents ++= Seq("mllib")

val localM2 = Path.userHome.absolutePath + "/.m2/repository"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),

  Resolver.mavenLocal
)

publishTo := Some(Resolver.mavenLocal)

javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")

parallelExecution in Test := false

// See http://stackoverflow.com/questions/28565837/filename-too-long-sbt
scalacOptions ++= Seq("-Xmax-classfile-name","78")

libraryDependencies ++= Seq(

  "org.scala-lang.modules" %% "scala-xml"        % "1.0.5",

  "com.esotericsoftware"   % "kryo"              % "4.0.0",

  "com.github.haifengl"    % "smile-core"        % "1.2.0",
  "io.reactivex"           %% "rxscala"          % "0.26.1",
  "org.scalaz"             %% "scalaz-core"      % "7.2.0",

  "com.github.karlhigley"  % "spark-neighbors_2.10"  % "0.3.6-FORK" exclude("org.apache.spark", "*") exclude("org.scalanlp", "*"),

  "com.softwaremill.quicklens" %% "quicklens"    % "1.4.8",

  "org.scalatest"          %% "scalatest"          % "3.0.1"       % "test",
  "com.holdenkarau"        %% "spark-testing-base" % "1.6.1_0.3.3" % "test"

)