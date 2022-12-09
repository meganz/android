package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow


/**
 * Get playing position histories
 */
fun interface GetPlayingPositionHistories {

    /**
     * Invoke
     *
     * @param key the name of key
     * @param default value to return is none set
     * @return current value and future updates as a flow
     */
    operator fun invoke(key: String?, default: String?): Flow<String?>
}