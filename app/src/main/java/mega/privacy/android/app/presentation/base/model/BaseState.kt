package mega.privacy.android.app.presentation.base.model

import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.AccountType

/**
 * UI state for [mega.privacy.android.app.BaseActivity].
 *
 * @property accountBlockedEvent [AccountBlockedEvent]
 * @property showExpiredBusinessAlert [Boolean]
 * @property accountType [AccountType]
 */
data class BaseState(
    val accountBlockedEvent: AccountBlockedEvent? = null,
    val showExpiredBusinessAlert: Boolean = false,
    val accountType: AccountType? = null,
)
