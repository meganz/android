package mega.privacy.android.app.presentation.hidenode

import mega.privacy.android.domain.entity.AccountType

internal data class HiddenNodesOnboardingState(
    val isInitialized: Boolean = false,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
)
