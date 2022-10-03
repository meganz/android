package mega.privacy.android.domain.entity

/**
 * Version update check result
 *
 */

sealed interface VersionUpdateCheckResult

/**
 * First version result
 *
 * @property currentVersionCode
 */
data class FirstVersionResult(val currentVersionCode: Int) : VersionUpdateCheckResult

/**
 * Same version result
 *
 * @property currentVersionCode
 */
data class SameVersionResult(val currentVersionCode: Int) : VersionUpdateCheckResult

/**
 * New version result
 *
 * @property oldVersionCode
 * @property newVersionCode
 */
data class NewVersionResult(val oldVersionCode: Int, val newVersionCode: Int) :
    VersionUpdateCheckResult