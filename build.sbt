name := "scalawithdb"
version := "1.0"
scalaVersion := "2.11.2"

libraryDependencies += "com.typesafe" % "config" % "1.3.2"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"         % "3.2.2",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "3.2.2",
  "com.h2database"  %  "h2"                  % "1.4.197",
  "ch.qos.logback"  %  "logback-classic"     % "1.2.3"
)
enablePlugins(ScalikejdbcPlugin)
libraryDependencies += "net.liftweb" %% "lift-json" % "3.2.0-M2"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.9"
resolvers += Resolver.sonatypeRepo("releases")
//libraryDependencies += "com.github.melrief" %% "purecsv" % "0.1.1"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3"
libraryDependencies += "com.github.seratch" %% "awscala" % "0.6.+"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.0"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.5"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.337"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
