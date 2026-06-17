package mega.privacy.android.feature.cloudexplorer.presentation.copy

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.picker.TargetNodePickerViewModel
import javax.inject.Inject

@HiltViewModel
internal class CopyViewModel @Inject constructor(
    getRootNodeIdUseCase: GetRootNodeIdUseCase,
    getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    private val getCopyLatestTargetUseCase: GetCopyLatestTargetUseCase,
) : TargetNodePickerViewModel(getRootNodeIdUseCase, getNodeNavigationStackUseCase) {

    override suspend fun getLatestTargetPath(): Long? = getCopyLatestTargetUseCase()
}
