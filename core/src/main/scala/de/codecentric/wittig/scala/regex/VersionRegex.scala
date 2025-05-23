package de.codecentric.wittig.scala.regex

object VersionRegex extends App:

  private val versionPattern = """(\d+)(?:\.(\d+)(?:\.(\d+))?)?""".r

  private def getVersion(value: String): Option[Version] = value match
    case versionPattern(major, null, null)   => Some(Version(major.toInt))
    case versionPattern(major, minor, null)  => Some(Version(major.toInt, minor.toInt))
    case versionPattern(major, minor, patch) => Some(Version(major.toInt, minor.toInt, patch.toInt))
    case _                                   => None

  assert(getVersion("1.2.3").contains(Version(1, 2, 3)))
  assert(getVersion("1.2").contains(Version(1, 2)))
  assert(getVersion("1").contains(Version(1)))
  assert(getVersion("1.").isEmpty)

case class Version(major: Int, minor: Int, patch: Int) {
  override def toString: String = this match
    case Version(minor = -1) => major.toString
    case Version(patch = -1) => s"$major.$minor"
    case _                   => s"$major.$minor.$patch"
}

object Version {
  def apply(major: Int, minor: Int): Version = Version(major, minor, -1)
  def apply(major: Int): Version             = Version(major, -1, -1)
}
