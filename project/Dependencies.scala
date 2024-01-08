import sbt._

object Versions {
  val cats           = "2.7.0"
  val catsEffect     = "3.3.5"
  val catsMtl        = "1.2.1"
  val fs2            = "3.2.4"
  val log4cats       = "2.2.0"
  val logback        = "1.2.10"
  val scala          = "2.13.8"
  val testcontainers = "1.16.3"
  val weaver         = "0.7.9"
}

object Dependencies {

  val catsCore       = "org.typelevel"       %% "cats-core"       % Versions.cats
  val catsEffect     = "org.typelevel"       %% "cats-effect"     % Versions.catsEffect
  val catsFree       = "org.typelevel"       %% "cats-free"       % Versions.cats
  val catsMtl        = "org.typelevel"       %% "cats-mtl"        % Versions.catsMtl
  val fs2Core        = "co.fs2"              %% "fs2-core"        % Versions.fs2
  val fs2Io          = "co.fs2"              %% "fs2-io"          % Versions.fs2
  val log4catsCore   = "org.typelevel"       %% "log4cats-core"   % Versions.log4cats
  val log4catsSlf4f  = "org.typelevel"       %% "log4cats-slf4j"  % Versions.log4cats
  val logback        = "ch.qos.logback"       % "logback-classic" % Versions.logback
  val testcontainers = "org.testcontainers"   % "testcontainers"  % Versions.testcontainers
  val weaver         = "com.disneystreaming" %% "weaver-cats"     % Versions.weaver

  val all: Seq[ModuleID] =
    Seq(
      catsCore,
      catsEffect,
      catsFree,
      catsMtl,
      fs2Core,
      fs2Io,
      log4catsCore,
      log4catsSlf4f,
      logback
    ) ++ Seq(
      testcontainers,
      weaver
    ).map(_ % "test")

}
