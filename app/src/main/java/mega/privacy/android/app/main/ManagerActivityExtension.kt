package mega.privacy.android.app.main

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import mega.privacy.android.app.presentation.manager.ManagerViewModel

fun ManagerActivity.observer(viewModel: ManagerViewModel) {
    lifecycleScope.launchWhenStarted {
        viewModel.onMyAvatarFileChanged
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collect {
                setProfileAvatar()
            }
    }
}