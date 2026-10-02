package mega.privacy.android.feature.sync.domain.entity.progress

/** What the sync transfer feed emits, in SDK order. */
internal sealed interface SyncTransferUpdate {

    /** The snapshot each feed collection starts with. */
    data class Seed(val entries: List<SyncTransferEntry>) : SyncTransferUpdate

    /** One chunk: one event per tag, sorted by notification number. */
    data class Batch(val events: List<SyncTransferEvent>) : SyncTransferUpdate

    /** A fresh snapshot; stored rows absent from it and at or below [watermark] are rolled back. */
    data class Reseed(val entries: List<SyncTransferEntry>, val watermark: Long) : SyncTransferUpdate
}
