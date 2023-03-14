package mega.privacy.android.app.presentation.featureflag

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.QAFeatures
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
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

    @Inject
    lateinit var getFeatureFlag: GetFeatureFlagValue

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext()).setView(
            ComposeView(requireContext()).apply {
                setContent {
                    val mode by getThemeMode()
                        .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                    AndroidTheme(isDark = mode.isDarkMode()) {
                        FeatureFlagBody { getFeatureFlag(QAFeatures.QATest) }
                    }
                }
            }
        ).setTitle(getString(R.string.qa_feature_flag_title))
            .create()

    /**
     * Calls the @FeatureFlagListContainer compose view to set the layout
     */
    @Composable
    fun FeatureFlagBody(isFeatureEnabled: suspend () -> Boolean) {
        val uiState by featureFlagMenuViewModel.state.collectAsStateWithLifecycle()

        val displayDescriptions by produceState(initialValue = false) {
            value = isFeatureEnabled()
        }


        Card(modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(1f)
            .fillMaxHeight(0.9f), shape = RoundedCornerShape(8.dp),
            content = {
                LazyColumn(
                    modifier = Modifier,
                    state = rememberSaveable(uiState,
                        saver = LazyListState.Saver) { LazyListState() }) {
                    items(items = uiState) { (name, description, isEnabled) ->
                        FeatureFlagRow(
                            name = name,
                            description = description.takeIf { displayDescriptions },
                            isEnabled = isEnabled,
                            onCheckedChange = featureFlagMenuViewModel::setFeatureEnabled,
                        )
                        Divider(color = Color.Black)
                    }
                }
            })
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
