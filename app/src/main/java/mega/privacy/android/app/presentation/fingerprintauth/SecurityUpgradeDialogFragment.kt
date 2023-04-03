package mega.privacy.android.app.presentation.fingerprintauth

import android.app.Dialog
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * SecurityUpgradeDialogFragment
 */
@AndroidEntryPoint
class SecurityUpgradeDialogFragment : DialogFragment() {

    private val securityUpgradeViewModel by viewModels<SecurityUpgradeViewModel>()

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * onCreateDialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        collectFlow(securityUpgradeViewModel.state) {
            if (it.shouldFinishScreen) {
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext()).setView(
            ComposeView(requireContext()).apply {
                setContent {
                    val mode by getThemeMode()
                        .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                    AndroidTheme(isDark = mode.isDarkMode()) {
                        SecurityUpgradeDialogView(
                            onCloseClick = {
                                dismiss()
                            },
                            onOkClick = {
                                securityUpgradeViewModel.upgradeAccountSecurity()
                            })
                    }
                }
            }
        ).create()
    }

    companion object {
        /**
         * Tag for logging
         */
        const val TAG = "SecurityUpgradeDialogFragment"

        /**
         * Creates instance of this class
         *
         * @return SecurityUpgradeDialogFragment new instance
         */
        fun newInstance() = SecurityUpgradeDialogFragment()
    }
}
