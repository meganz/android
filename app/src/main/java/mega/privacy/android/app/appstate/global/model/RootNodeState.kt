package mega.privacy.android.app.appstate.global.model

import mega.privacy.android.domain.entity.node.root.RefreshEvent

data class RootNodeState(
    val exists: Boolean = false,
    val refreshEvent: RefreshEvent? = null
)