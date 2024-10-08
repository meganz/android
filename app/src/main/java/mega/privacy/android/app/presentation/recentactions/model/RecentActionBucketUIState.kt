package mega.privacy.android.app.presentation.recentactions.model

import mega.privacy.android.domain.entity.AccountType


data class RecentActionBucketUIState(
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean? = null,
    val isBusinessAccountExpired: Boolean = false,
)