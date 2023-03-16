package mega.privacy.android.app.main.megachat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.chat.recent.RecentChatsViewModel

/**
 * observer [RecentChatsState]
 */
fun RecentChatsFragment.observer(viewModel: RecentChatsViewModel) {
    lifecycleScope.launch {
        viewModel.state.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect {
                RecentChatsFragment.isExpand = it.shouldShowRequestContactAccess
                expandContainer()
            }
    }
}