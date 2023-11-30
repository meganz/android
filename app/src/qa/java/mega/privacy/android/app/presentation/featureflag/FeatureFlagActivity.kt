package mega.privacy.android.app.presentation.featureflag

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlag
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Dialog to show feature flag view
 */
@AndroidEntryPoint
class FeatureFlagActivity : BaseActivity() {

    private val featureFlagMenuViewModel by viewModels<FeatureFlagMenuViewModel>()

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * On create
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val mode by getThemeMode()
                        .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                    val uiState by featureFlagMenuViewModel.state.collectAsStateWithLifecycle()
                    MegaAppTheme(isDark = mode.isDarkMode()) {
                        FeatureFlagBody(
                            featureFlags = uiState.filteredFeatureFlags,
                            onFeatureFlagChecked = featureFlagMenuViewModel::setFeatureEnabled,
                            displayDescriptions = uiState.showDescription,
                            filter = uiState.filter,
                            onFilterChanged = featureFlagMenuViewModel::onFilterChanged
                        )
                    }
                }
            }
        )
    }

    /**
     * Calls the @FeatureFlagListContainer compose view to set the layout
     */
    @Composable
    fun FeatureFlagBody(
        featureFlags: List<FeatureFlag>,
        onFeatureFlagChecked: (String, Boolean) -> Unit,
        displayDescriptions: Boolean,
        filter: String?,
        onFilterChanged: (String) -> Unit,
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(1f)
                .fillMaxHeight(0.9f),
            state = listState,
        ) {
            item {
                GenericTextField(
                    placeholder = "filter",
                    imeAction = ImeAction.Done,
                    onTextChange = onFilterChanged,
                    keyboardActions = KeyboardActions.Default,
                    text = filter ?: "",
                )
            }

            items(
                items = featureFlags,
                key = { it.featureName }
            ) { (name, description, isEnabled) ->
                FeatureFlagRow(
                    name = name,
                    description = description.takeIf { displayDescriptions },
                    isEnabled = isEnabled,
                    onCheckedChange = onFeatureFlagChecked,
                )
                Divider(color = Color.Black)
            }
        }
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
        fun newInstance() = FeatureFlagActivity()
    }
}
