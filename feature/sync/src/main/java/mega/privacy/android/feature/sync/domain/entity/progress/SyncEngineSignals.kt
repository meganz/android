package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * The SDK's waiting flag (any non-zero retry reason) and its live sync stall flag.
 * @property isWaiting - true while the SDK reports any non-zero waiting reason
 * @property isSyncStalled - true while the SDK reports the syncs as stalled
 */
data class SyncEngineSignals(val isWaiting: Boolean, val isSyncStalled: Boolean) {
    companion object {
        /** Neither waiting nor stalled; also the value a failed read yields. */
        val NotWaiting = SyncEngineSignals(isWaiting = false, isSyncStalled = false)
    }
}
