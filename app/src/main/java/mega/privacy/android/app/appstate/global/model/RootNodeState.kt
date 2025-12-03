package mega.privacy.android.app.appstate.global.model

data class RootNodeState(
    val exists: Boolean = false,
    val refreshEvent: RefreshEvent? = null
)