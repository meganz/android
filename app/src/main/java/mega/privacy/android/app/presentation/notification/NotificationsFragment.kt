package mega.privacy.android.app.presentation.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.notification.view.NotificationView
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import javax.inject.Inject

/**
 * NotificationsFragment
 */
@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private val viewModel: NotificationViewModel by viewModels()

    /**
     * Get Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = mode.isDarkMode()) {
                    NotificationView(viewModel)
                }
            }
        }
    }

    /**
     * NotificationView
     */
    @Composable
    fun NotificationView(viewModel: NotificationViewModel) {
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        (activity as ManagerActivity?)?.let {
            NotificationView(uiState, onClick = { notification ->
                notification.onClick(it)
            }, onNotificationsLoaded = {
                viewModel.onNotificationsLoaded()
            })
        }
    }

    companion object {
        /**
         * New Instance
         */
        @JvmStatic
        fun newInstance(): NotificationsFragment = NotificationsFragment()
    }
}