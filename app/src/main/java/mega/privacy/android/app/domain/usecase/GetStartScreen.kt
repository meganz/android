package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Get start screen
 *
 */
fun interface GetStartScreen {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Int>
}