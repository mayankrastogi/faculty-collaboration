import scala.sys.process.Process

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
  "org.apache.hadoop" % "hadoop-common" % "3.2.0" exclude("org.slf4j", "slf4j-log4j12"),
//  "org.apache.hadoop" % "hadoop-client" % "3.2.0" exclude("org.slf4j", "slf4j-log4j12"),
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.2.0" exclude("org.slf4j", "slf4j-log4j12"),
  
  // Scala XML module
  "org.scala-lang.modules" %% "scala-xml" % "1.1.1",

  // Scalatest testing framework
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
)

// Create a deployment task to automate copying the jar file to HDP sandbox and then starting the job.

lazy val deploy = taskKey[Unit]("Deploys jar file to sandbox and runs the map reduce job.")

deploy := {
  val log = streams.value.log

  log.info("Starting deploy task...")

  // Use Windows Subsytem for Linux (WSL) to run the shell script
  val process = Process("wsl ./deploy.sh").run(log)
  val exitCode = process.exitValue()

  if(exitCode == 0) {
    log.success("Deploy task completed successfully ")
  }
  else {
    log.error("Deploy task failed with exit code " + exitCode)
  }
}