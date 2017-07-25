lazy val commonSettings = Seq(
  version := "0.1",
  organization := "io.hydrosphere",
  scalaVersion := "2.11.11",
  test in assembly := {}
)

lazy val servingLib = project.in(file("serving_scala"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )

lazy val example = project.in(file("example"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "example.jar"
  )
  .dependsOn(servingLib)

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .dependsOn(servingLib)
  .aggregate(example)
  .settings(
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    assemblyJarName in assembly := "server.jar",
    mainClass in assembly := Some("io.hydrosphere.serving.runtime.scala.Main")
  )