package mega.privacy.android.app.presentation.recentactions.model

import mega.privacy.android.domain.entity.account.AccountDetail

data class RecentActionBucketUIState(
    val accountDetail: AccountDetail? = null,
    val isHiddenNodesOnboarded: Boolean? = null,
)