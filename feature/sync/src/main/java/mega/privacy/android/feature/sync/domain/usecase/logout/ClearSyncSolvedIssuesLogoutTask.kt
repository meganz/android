package mega.privacy.android.feature.sync.domain.usecase.logout

import mega.privacy.android.domain.usecase.logout.LogoutTask
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSyncSolvedIssuesUseCase
import javax.inject.Inject

internal class ClearSyncSolvedIssuesLogoutTask @Inject constructor(
    private val clearSyncSolvedIssuesUseCase: ClearSyncSolvedIssuesUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearSyncSolvedIssuesUseCase()
    }
}