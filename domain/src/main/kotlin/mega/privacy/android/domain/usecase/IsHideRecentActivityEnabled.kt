package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Is hide recent activity enabled
 *
 */
fun interface IsHideRecentActivityEnabled {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}