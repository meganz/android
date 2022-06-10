package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow


/**
 * Get preference
 *
 * @param T preference type
 */
fun interface GetPreference<T> {


    /**
     * Invoke
     *
     * @param key name of the preference
     * @param default value to return if none set
     * @return current value of the preference and future updates as a flow
     */
    operator fun invoke(key: String?, default: T): Flow<T>
}
