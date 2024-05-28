package mega.privacy.android.app.presentation.notification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.notification.view.NotificationView
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
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
                OriginalTempTheme(isDark = mode.isDarkMode()) {
                    NotificationView(viewModel)
                }
            }
        }
    }

    /**
     * NotificationView
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun NotificationView(viewModel: NotificationViewModel) {
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        (activity as ManagerActivity?)?.let {
            NotificationView(
                state = uiState,
                onNotificationClick = { notification ->
                    notification.onClick(it)
                },
                onPromoNotificationClick = { promoNotification ->
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(promoNotification.actionURL))
                    if (intent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Timber.d("No Application found to can handle promo notification intent")
                    }
                },
                onNotificationsLoaded = {
                    viewModel.onNotificationsLoaded()
                },
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                }
            )
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