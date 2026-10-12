package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * One input for the store's run, stamped with the store generation it was issued in.
 * The run drops a command from an older generation, so a reset is never refilled.
 */
internal data class SyncTransferStoreCommand(
    val generation: Long,
    val input: SyncTransferStoreInput,
)
