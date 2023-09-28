package mega.privacy.android.app.presentation.base.model

import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState

/**
 * UI state for [mega.privacy.android.app.BaseActivity].
 *
 * @property transfersFinished [TransfersFinishedState]
 * @property accountBlockedDetail [AccountBlockedDetail]
 * @property showExpiredBusinessAlert [Boolean]
 */
data class BaseState(
    val transfersFinished: TransfersFinishedState? = null,
    val accountBlockedDetail: AccountBlockedDetail? = null,
    val showExpiredBusinessAlert: Boolean = false,
)
