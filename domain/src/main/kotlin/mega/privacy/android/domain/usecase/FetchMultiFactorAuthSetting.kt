package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Fetch multi factor auth setting
 *
 */
fun interface FetchMultiFactorAuthSetting {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}