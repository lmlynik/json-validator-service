ThisBuild / scalaVersion     := "3.1.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "pl.lmlynik"

lazy val root = (project in file("."))
  .settings(
    name := "json-validator-service",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.0",
      "dev.zio" %% "zio-test" % "2.0.0" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.0.0" % Test,
      "io.d11" %% "zhttp"      % "2.0.0-RC10",
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.0.3",
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.0.3",
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.0.3",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.0.3",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.0.3",
      "com.github.java-json-tools" % "json-schema-validator" % "2.2.14"

    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
