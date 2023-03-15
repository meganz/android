package mega.privacy.android.domain.usecase.login

/**
 * Use case for broadcast fetch nodes finish.
 */
fun interface BroadcastFetchNodesFinish {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}