package mega.privacy.android.app.presentation.tags

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Tags screen activity.
 */
@AndroidEntryPoint
class TagsActivity : BaseActivity() {

    /**
     * GetThemeMode use case.
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: TagsViewModel by viewModels()

    /**
     * Create the Tags screen.
     *
     * @param savedInstanceState    Saved instance state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            OriginalTempTheme(themeMode.isDarkMode()) {
                TagsScreen(
                    addNodeTag = viewModel::addNodeTag,
                    consumeInfoMessage = viewModel::consumeInfoMessage,
                    validateTagName = viewModel::validateTagName,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    uiState = uiState
                )
            }
        }
    }

    companion object {
        const val NODE_ID = "nodeId"
    }
}