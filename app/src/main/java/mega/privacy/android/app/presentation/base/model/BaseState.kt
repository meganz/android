package mega.privacy.android.app.presentation.base.model

import mega.privacy.android.domain.entity.AccountBlockedEvent

/**
 * UI state for [mega.privacy.android.app.BaseActivity].
 *
 * @property accountBlockedEvent [AccountBlockedEvent]
 * @property showExpiredBusinessAlert [Boolean]
 */
data class BaseState(
    val accountBlockedEvent: AccountBlockedEvent? = null,
    val showExpiredBusinessAlert: Boolean = false,
)
