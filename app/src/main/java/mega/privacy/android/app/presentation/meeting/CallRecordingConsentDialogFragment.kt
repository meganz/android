package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.chat.ChatViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.view.CallRecordingConsentDialog
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Call Recording Consent Dialog Fragment
 * Necessary to display the compose dialog in ChatActivity.java
 */
@AndroidEntryPoint
class CallRecordingConsentDialogFragment : DialogFragment() {
    private val viewModel: ChatViewModel by viewModels()

    /**
     * GetThemeMode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * On create view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                MegaAppTheme(isDark = isDark) {
                    CallRecordingConsentDialog(
                        onConfirm = {
                            viewModel.setIsRecordingConsentAccepted(value = true)
                            viewModel.setShowRecordingConsentDialogConsumed()
                            dismissAllowingStateLoss()
                        },
                        onDismiss = {
                            viewModel.setIsRecordingConsentAccepted(value = false)
                            viewModel.setShowRecordingConsentDialogConsumed()
                            dismissAllowingStateLoss()
                        },
                        onLearnMore = {
                            val viewIntent = Intent(Intent.ACTION_VIEW)
                            viewIntent.data = Uri.parse("https://mega.io/privacy")
                            startActivity(viewIntent)
                        }
                    )
                }
            }
        }
    }

    companion object {
        /**
         * New instance
         */
        fun newInstance(): CallRecordingConsentDialogFragment = CallRecordingConsentDialogFragment()
    }
}