package mega.privacy.android.domain.entity.login

/**
 * Enum class for defining the possible temporary errors related to waiting and retry
 */
enum class TemporaryWaitingError {

    /**
     * Connectivity issues.
     */
    ConnectivityIssues,

    /**
     * Server issues.
     */
    ServerIssues,

    /**
     * Api lock.
     */
    APILock,

    /**
     * Api rate.
     */
    APIRate,
}