package mega.privacy.android.app.presentation.featureflag

import android.app.Dialog
import android.os.Bundle
import androidx.compose.runtime.Composable
import collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Dialog to show feature flag view
 */
@AndroidEntryPoint
class FeatureFlagDialogFragment : DialogFragment() {

    private val featureFlagMenuViewModel by viewModels<FeatureFlagMenuViewModel>({ requireParentFragment() })

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext()).setView(
            ComposeView(requireContext()).apply {
                setContent {
                    val mode by getThemeMode()
                        .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                    AndroidTheme(isDark = mode.isDarkMode()) {
                        FeatureFlagBody()
                    }
                }
            }
        ).setTitle(getString(R.string.qa_feature_flag_title))
            .create()

    /**
     * Calls the @FeatureFlagListContainer compose view to set the layout
     */
    @Composable
    fun FeatureFlagBody() {
        val uiState by featureFlagMenuViewModel.state.collectAsStateWithLifecycle()
        FeatureFlagListContainer(
            uiState = uiState,
            onCheckedChange = featureFlagMenuViewModel::setFeatureEnabled
        )
    }

    companion object {
        /**
         * Tag for logging
         */
        const val TAG = "FeatureFlagDialogFragment"

        /**
         * Creates instance of this class
         *
         * @return FeatureFlagDialogFragment new instance
         */
        fun newInstance() = FeatureFlagDialogFragment()
    }
}
