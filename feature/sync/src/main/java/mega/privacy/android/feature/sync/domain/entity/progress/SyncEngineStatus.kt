package mega.privacy.android.feature.sync.domain.entity.progress

/** What the sync engine is busy with, in desktop's busy order. */
internal enum class SyncEngineWork { Scanning, Syncing, Waiting, Transferring }

/** Why the sync engine status reads Paused. */
internal sealed interface SyncEnginePauseCause {

    /** The app-wide transfer pause. */
    data object TransfersPaused : SyncEnginePauseCause

    /**
     * The syncs themselves are paused.
     * @property cause The episode's pause cause; User after Pause all
     */
    data class Syncs(val cause: SyncPauseCause) : SyncEnginePauseCause
}

/** The sync status for the whole app, following desktop's status chain. */
internal sealed interface SyncEngineStatus {

    /** Not every input is known yet. */
    data object NotSeeded : SyncEngineStatus

    /** No connectivity. */
    data object Offline : SyncEngineStatus

    /** No sync is configured. */
    data object NoSyncs : SyncEngineStatus

    /**
     * The syncs are stalled with issues listed.
     * @property issueCount The number of listed issues
     */
    data class IssuesFound(val issueCount: Int) : SyncEngineStatus

    /**
     * Paused.
     * @property cause The app-wide pause or the syncs' own pause
     */
    data class Paused(val cause: SyncEnginePauseCause) : SyncEngineStatus

    /**
     * Busy.
     * @property work The first busy work in desktop's order
     */
    data class Busy(val work: SyncEngineWork) : SyncEngineStatus

    /**
     * Nothing to do.
     * @property isAccountFull True while the storage is over quota
     */
    data class UpToDate(val isAccountFull: Boolean) : SyncEngineStatus
}
