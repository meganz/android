package mega.privacy.android.feature.cloudexplorer.presentation.move

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.picker.TargetNodePickerViewModel
import javax.inject.Inject

@HiltViewModel
internal class MoveViewModel @Inject constructor(
    getRootNodeIdUseCase: GetRootNodeIdUseCase,
    getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    private val getMoveLatestTargetUseCase: GetMoveLatestTargetUseCase,
) : TargetNodePickerViewModel(getRootNodeIdUseCase, getNodeNavigationStackUseCase) {

    override suspend fun getLatestTargetPath(): Long? = getMoveLatestTargetUseCase()
}
