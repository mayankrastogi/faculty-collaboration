name := "mayank_k_rastogi_hw2"

version := "0.1"

scalaVersion := "2.12.8"

// Merge strategy to avoid deduplicate errors
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

libraryDependencies ++= Seq(
  // Typesafe Configuration Library
  "com.typesafe" % "config" % "1.3.2",

  // Logback logging framework
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.gnieh" % "logback-config" % "0.3.1",
  
  // Apache Hadoop
  "org.apache.hadoop" % "hadoop-common" % "3.2.0",
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.2.0",
  
  // JUnit testing framework
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test->default",
)