import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "uk.gov.hmrc.agentregistration.shared.*",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "uk.gov.hmrc.agentregistrationrisking.testOnly.*"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageEnabled := true,
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )

}
