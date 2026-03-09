package mega.privacy.android.domain.entity

/**
 * App version
 *
 * @property major Major version number
 * @property minor Minor version number
 * @property patch Patch version number
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int?,
) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, AppVersion::major, AppVersion::minor)
}
