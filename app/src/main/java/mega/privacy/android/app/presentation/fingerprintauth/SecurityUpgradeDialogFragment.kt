package mega.privacy.android.app.presentation.fingerprintauth

import android.app.Dialog
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import javax.inject.Inject

/**
 * SecurityUpgradeDialogFragment
 */
@AndroidEntryPoint
class SecurityUpgradeDialogFragment : DialogFragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext()).setView(
            ComposeView(requireContext()).apply {
                setContent {
                    val nodeName = arguments?.getString("nodeName", "Default") as String
                    val mode by getThemeMode()
                        .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                    AndroidTheme(isDark = mode.isDarkMode()) {
                        SecurityUpgradeDialogView(folderName = nodeName, onCancelClick = {
                            dismiss()
                        }, onOkClick = {
                            dismiss()
                        })
                    }
                }
            }
        ).create()

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