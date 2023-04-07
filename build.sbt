val tapirVersion = "1.2.10"

lazy val rootProject = (project in file("."))
  .settings(
    Seq(
      name := "ls-lab-4-sample-project",
      version := "0.1.0-SNAPSHOT",
      organization := "timeserver",
      scalaVersion := "3.2.2",
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
        "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
        "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
        "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
        "com.outr" %% "scribe" % "3.11.1",
        "com.outr" %% "scribe-slf4j" % "3.11.1",
        "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
        "com.github.pureconfig" %% "pureconfig-core" % "0.17.2",
        "io.monix" %% "newtypes-core" % "0.2.3",
        "dev.zio" %% "zio-test" % "2.0.10" % Test,
        "dev.zio" %% "zio-test-sbt" % "2.0.10" % Test,
        "com.softwaremill.sttp.client3" %% "circe" % "3.8.13" % Test
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    Docker / version := "latest",
    Docker / packageName := "ls-lab-4-sample-project",
    Docker / maintainer := "Mihail Olokin",
    dockerBaseImage := "openjdk:17",
    dockerExposedPorts := Seq(8000, 8080),
    dockerUsername := Some("nikololiahim"),
  )
