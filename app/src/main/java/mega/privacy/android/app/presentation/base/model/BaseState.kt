package mega.privacy.android.app.presentation.base.model

import mega.privacy.android.domain.entity.transfer.TransfersFinishedState

/**
 * UI state for [mega.privacy.android.app.BaseActivity].
 *
 * @property transfersFinished [TransfersFinishedState]
 */
data class BaseState(
    val transfersFinished: TransfersFinishedState? = null,
)
