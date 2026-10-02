package mega.privacy.android.feature.sync.domain.entity.progress

/** The SDK transfer callback that produced a sync transfer event. */
internal enum class SyncTransferEventType { Start, Update, TemporaryError, Finish }
