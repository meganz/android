package mega.privacy.android.app.presentation.tags

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Tags screen activity.
 */
@AndroidEntryPoint
class TagsActivity : AppCompatActivity() {

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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = themeMode.isDarkMode().not()
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = useDarkIcons
            )
            SessionContainer {
                OriginalTempTheme(themeMode.isDarkMode()) {
                    TagsScreen(
                        consumeInfoMessage = viewModel::consumeInfoMessage,
                        validateTagName = viewModel::validateTagName,
                        onBackPressed = onBackPressedDispatcher::onBackPressed,
                        addOrRemoveTag = viewModel::addOrRemoveTag,
                        consumeMaxTagsError = viewModel::consumeMaxTagsError,
                        consumeTagsUpdated = viewModel::consumeTagsUpdatedEvent,
                        uiState = uiState
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Node ID extra key.
         */
        const val NODE_ID = "nodeId"

        /**
         * Maximum characters per tag.
         */
        const val MAX_CHARS_PER_TAG = 32

        /**
         * Maximum tags per node.
         */
        const val MAX_TAGS_PER_NODE = 10
    }
}