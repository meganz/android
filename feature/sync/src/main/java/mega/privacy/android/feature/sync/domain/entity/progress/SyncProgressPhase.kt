package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * The phase of the current sync run, shared by every sync progress surface.
 */
internal sealed interface SyncProgressPhase {

    /**
     * No sync is configured
     */
    data object NoSyncs : SyncProgressPhase

    /**
     * The syncs are scanning or starting, with nothing pending yet
     */
    data object Checking : SyncProgressPhase

    /**
     * Files are pending
     */
    data object Transferring : SyncProgressPhase

    /**
     * The syncs are paused
     *
     * @property cause Why they are paused
     */
    data class Paused(val cause: SyncPauseCause) : SyncProgressPhase

    /**
     * A run that processed files has just finished
     */
    data object Completed : SyncProgressPhase

    /**
     * Nothing is left to do
     */
    data object UpToDate : SyncProgressPhase
}
