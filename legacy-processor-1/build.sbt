name := "legacy-processor-1"

version := "0.1"

scalaVersion := "2.13.4"

val AkkaVersion = "2.6.10"
val AlpakkaVersion = "2.0.5"
val RSocketVersion = "1.1.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "org.slf4j" % "slf4j-simple" % "1.7.28", // TODO use nop
  "io.rsocket" % "rsocket-core" % RSocketVersion,
  "io.rsocket" % "rsocket-transport-netty" % RSocketVersion
)