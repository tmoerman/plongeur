import sbt.Keys._

organization := "org.tmoerman"

name := "plongeur-spark"

description := "Plongeur Spark algorithms module"

scalaVersion := "2.11.8"

val localM2 = Path.userHome.absolutePath + "/.m2/repository"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),

  "local .m2" at "file://" + localM2
)

val sparkVersion  = "2.0.0"

publishTo := Some(Resolver.file("file",  new File(localM2)))

libraryDependencies ++= Seq(

  "org.apache.spark"       %% "spark-core"           % sparkVersion % "provided" exclude("org.scalanlp", "*"),
  "org.apache.spark"       %% "spark-sql"            % sparkVersion              exclude("org.scalanlp", "*"),
  "org.apache.spark"       %% "spark-mllib"          % sparkVersion              exclude("org.scalanlp", "*"),
  "com.esotericsoftware"   % "kryo"                  % "4.0.0",

  "org.scalanlp"           % "breeze_2.10"           % "0.12",
  "org.scalanlp"           % "breeze-natives_2.10"   % "0.12",

  "com.github.haifengl"    % "smile-core"            % "1.1.0",

  "io.reactivex"           %% "rxscala"              % "0.26.2",
  "org.scalaz"             %% "scalaz-core"          % "7.2.0",

  "com.github.karlhigley"  % "spark-neighbors_2.10"  % "0.3.0-FORK",

  "com.softwaremill.quicklens" %% "quicklens"        % "1.4.7",

  "org.scalatest"          %% "scalatest"            % "2.2.4"      % "test"

)

// unmanagedJars in Test += file("lib_bak/toree-assembly-0.2.0.dev1-incubating-SNAPSHOT.jar")