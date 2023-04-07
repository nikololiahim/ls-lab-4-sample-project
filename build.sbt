val tapirVersion = "1.2.10"

lazy val rootProject = (project in file(".")).settings(
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
      "ch.qos.logback" % "logback-classic" % "1.4.6",
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
