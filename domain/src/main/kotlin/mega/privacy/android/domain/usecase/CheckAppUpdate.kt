package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.VersionUpdateCheckResult

/**
 * Check app update
 *
 */
fun interface CheckAppUpdate {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(): VersionUpdateCheckResult
}