package mega.privacy.android.core.nodecomponents.model

data class RestoreData(
    val message: String,
    val parentHandle: Long,
    val restoredNodeHandle: Long,
)