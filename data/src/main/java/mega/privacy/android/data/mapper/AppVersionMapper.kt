package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AppVersion
import javax.inject.Inject

/**
 * Mapper to convert between [String] version format and [AppVersion]
 */
internal class AppVersionMapper @Inject constructor() {

    /**
     * Converts a version string (e.g. "14.2.1") to [AppVersion], ignoring the patch component.
     * Returns null if the string cannot be parsed.
     */
    operator fun invoke(version: String): AppVersion? {
        val parts = version.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val patch = parts.getOrNull(2)?.toIntOrNull()
        return AppVersion(major = major, minor = minor, patch = patch)
    }

    /**
     * Converts [AppVersion] to its string representation (e.g. "14.2.1")
     */
    operator fun invoke(version: AppVersion) =
        buildString {
            append(version.major)
            append('.')
            append(version.minor)
            version.patch?.let {
                append('.')
                append(it)
            }
        }
}
