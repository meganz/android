package mega.privacy.android.app.presentation.settings.customisenavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.app.presentation.settings.customisenavigation.model.CustomiseNavigationUiState
import mega.privacy.android.app.presentation.settings.customisenavigation.model.MaxSelectableNavigationItems
import mega.privacy.android.app.presentation.settings.customisenavigation.model.MinSelectableNavigationItems
import mega.privacy.android.app.presentation.settings.customisenavigation.model.NavigationItemUiModel
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Baselines for [CustomiseNavigationScreen]: the default arrangement, the full
 * selection with the counter in its error state, and the max/min-items snackbars
 * shown when saving an invalid selection.
 */
class CustomiseNavigationScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CustomiseNavigationScreenDefaultArrangement() {
        AndroidThemeForPreviews {
            CustomiseNavigationScreen(
                state = data(
                    selected = listOf(home, drive, media),
                    available = listOf(chat, shares),
                ),
                onBackPressed = {},
                onSave = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CustomiseNavigationScreenFullSelection() {
        AndroidThemeForPreviews {
            CustomiseNavigationScreen(
                state = data(
                    selected = listOf(chat, drive, media, home),
                    available = listOf(shares),
                ),
                onBackPressed = {},
                onSave = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CustomiseNavigationScreenMaxItemsSnackbar() {
        AndroidThemeForPreviews {
            ScreenWithSnackbar(
                message = stringResource(
                    sharedR.string.settings_customise_navigation_max_items_snackbar,
                    MaxSelectableNavigationItems,
                ),
            ) {
                CustomiseNavigationScreen(
                    state = data(
                        selected = listOf(home, drive, media, chat, shares),
                        available = emptyList(),
                    ),
                    onBackPressed = {},
                    onSave = {},
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CustomiseNavigationScreenMinItemsSnackbar() {
        AndroidThemeForPreviews {
            ScreenWithSnackbar(
                message = stringResource(
                    sharedR.string.settings_customise_navigation_min_items_snackbar,
                    MinSelectableNavigationItems,
                ),
            ) {
                CustomiseNavigationScreen(
                    state = data(
                        selected = listOf(home, drive),
                        available = listOf(media, chat, shares),
                    ),
                    onBackPressed = {},
                    onSave = {},
                )
            }
        }
    }

    /**
     * Renders [content] with a MEGA-styled snackbar pinned to the bottom.
     *
     * The production snackbar goes through a [androidx.compose.material3.SnackbarHost]
     * that shows nothing until a `showSnackbar` call animates it in, so it cannot be
     * captured in a static preview. This mirrors core-ui `MegaSnackbar` styling on a
     * plain slot-based [Snackbar] instead.
     */
    @Composable
    private fun ScreenWithSnackbar(
        message: String,
        content: @Composable () -> Unit,
    ) {
        Box {
            content()
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = LocalSpacing.current.x8,
                        end = LocalSpacing.current.x8,
                        bottom = LocalSpacing.current.x12,
                    ),
                containerColor = DSTokens.colors.components.toastBackground,
                contentColor = DSTokens.colors.text.inverse,
            ) {
                Text(text = message)
            }
        }
    }

    private fun data(
        selected: List<NavigationItemUiModel>,
        available: List<NavigationItemUiModel>,
    ) = CustomiseNavigationUiState.Data(
        baseArrangement = selected,
        availableItems = available,
        menuItem = menu,
        defaultArrangementIds = listOf("home", "drive", "media"),
        savedEvent = consumed,
    )

    private val home = NavigationItemUiModel(
        id = "home",
        label = sharedR.string.general_section_home,
        icon = IconPack.Medium.Thin.Outline.Home,
    )
    private val drive = NavigationItemUiModel(
        id = "drive",
        label = sharedR.string.general_drive,
        icon = IconPack.Medium.Thin.Outline.Folder,
    )
    private val media = NavigationItemUiModel(
        id = "media",
        label = sharedR.string.media_feature_title,
        icon = IconPack.Medium.Thin.Outline.Image01,
    )
    private val chat = NavigationItemUiModel(
        id = "chat",
        label = sharedR.string.general_chat,
        icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
    )
    private val shares = NavigationItemUiModel(
        id = "shares",
        label = sharedR.string.video_section_videos_location_option_shared_items,
        icon = IconPack.Medium.Thin.Outline.FolderUsers,
    )
    private val menu = NavigationItemUiModel(
        id = "menu",
        label = sharedR.string.general_menu,
        icon = IconPack.Medium.Thin.Outline.Menu01,
    )
}
