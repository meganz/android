package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper to waiting error code to enum
 */
internal class TemporaryWaitingErrorMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param errorCode [Int]
     */
    operator fun invoke(errorCode: Int) =
        when (errorCode) {
            MegaApiJava.RETRY_NONE -> null

            MegaApiJava.RETRY_CONNECTIVITY -> {
                TemporaryWaitingError.ConnectivityIssues
            }

            MegaApiJava.RETRY_SERVERS_BUSY -> {
                TemporaryWaitingError.ServerIssues
            }

            MegaApiJava.RETRY_API_LOCK -> {
                TemporaryWaitingError.APILock
            }

            MegaApiJava.RETRY_RATE_LIMIT -> {
                TemporaryWaitingError.APIRate
            }

            else -> {
                TemporaryWaitingError.ConnectivityIssues
            }
        }
}
