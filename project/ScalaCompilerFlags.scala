object ScalaCompilerFlags {

  val scalaCompilerOptions: Seq[String] = Seq(
//    "-explain",
    "-explain-cyclic",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-Wconf:msg=While parsing annotations in:silent",
    "-Yno-flexible-types",
    "-Wconf:src=target/.*:s"
  )

  val strictScalaCompilerOptions: Seq[String] = Seq(
    "-Xfatal-warnings",
    "-Wvalue-discard",
    "-feature",
  )
}
