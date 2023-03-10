package mega.privacy.android.domain.entity.login

/**
 * Enum class for defining the possible temporary errors for a fetch nodes.
 */
enum class FetchNodesTemporaryError {

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