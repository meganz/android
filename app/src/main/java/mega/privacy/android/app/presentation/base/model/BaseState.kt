package mega.privacy.android.app.presentation.base.model

import mega.privacy.android.domain.entity.account.AccountBlockedDetail

/**
 * UI state for [mega.privacy.android.app.BaseActivity].
 *
 * @property accountBlockedDetail [AccountBlockedDetail]
 * @property showExpiredBusinessAlert [Boolean]
 */
data class BaseState(
    val accountBlockedDetail: AccountBlockedDetail? = null,
    val showExpiredBusinessAlert: Boolean = false,
)
