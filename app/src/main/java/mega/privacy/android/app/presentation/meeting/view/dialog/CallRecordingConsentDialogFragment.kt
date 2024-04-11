package mega.privacy.android.app.presentation.meeting.view.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import javax.inject.Inject

/**
 * Call Recording Consent Dialog Fragment
 * Necessary to display the compose dialog in ChatActivity.java
 */
@AndroidEntryPoint
@Deprecated("Use only in case of Java code. Use CallRecordingConsentDialog compose view instead.")
class CallRecordingConsentDialogFragment : DialogFragment() {

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
                        onDismiss = { dismissAllowingStateLoss() }
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