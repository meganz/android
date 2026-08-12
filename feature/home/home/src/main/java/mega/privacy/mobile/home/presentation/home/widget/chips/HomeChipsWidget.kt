package mega.privacy.mobile.home.presentation.home.widget.chips

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetOrder
import mega.privacy.android.navigation.destination.AudioSectionNavKey
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.FavouritesNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AudiosChipButtonPressedEvent
import mega.privacy.mobile.analytics.event.ChatChipButtonPressedEvent
import mega.privacy.mobile.analytics.event.FavouritesChipButtonPressedEvent
import mega.privacy.mobile.analytics.event.OfflineChipButtonPressedEvent
import javax.inject.Inject

class HomeChipsWidget @Inject constructor(
) : HomeWidget {

    override val identifier: String = "HomeChipsWidgetProvider"
    override val defaultOrder: HomeWidgetOrder = HomeWidgetOrder.Shortcuts
    override val canDelete: Boolean = false
    override val isConfigurable: Boolean = true
    override val isDraggable: Boolean = false
    override suspend fun getWidgetName() =
        LocalizedText.StringRes(sharedR.string.home_configuration_widget_shortcuts_widget_name)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        val viewModel = hiltViewModel<HomeChipsWidgetViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        HomeChips(
            hiddenSectionIds = uiState.hiddenSectionIds,
            onNavigate = navigationHandler::navigate,
            modifier = modifier,
        )
    }
}

/**
 * Bottom navigation section ids ([mega.privacy.android.navigation.contract.MainNavItem.id]) for
 * the chips that have a bottom navigation bar equivalent. A chip is hidden when its section is in
 * the bar, as it would be a redundant shortcut. The Audios chip has no such id because there is no
 * Audios navigation item, so it can never be in the bar and is always shown.
 */
internal object HomeChipSectionIds {
    const val FAVOURITES = "favourites"
    const val OFFLINE = "offline"
    const val CHAT = "chat"
}

@Composable
internal fun HomeChips(
    hiddenSectionIds: Set<String>,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.size(8.dp)) // Total 16dp with spacedBy

        if (HomeChipSectionIds.FAVOURITES !in hiddenSectionIds) {
            MegaChip(
                content = stringResource(sharedR.string.video_section_title_favourite_playlist),
                selected = false,
                leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.Heart),
                onClick = {
                    Analytics.tracker.trackEvent(FavouritesChipButtonPressedEvent)
                    onNavigate(FavouritesNavKey)
                },
            )
        }
        MegaChip(
            content = stringResource(sharedR.string.home_screen_audios_chip_title),
            selected = false,
            leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.Music),
            onClick = {
                Analytics.tracker.trackEvent(AudiosChipButtonPressedEvent)
                onNavigate(AudioSectionNavKey)
            },
        )
        if (HomeChipSectionIds.OFFLINE !in hiddenSectionIds) {
            MegaChip(
                content = stringResource(sharedR.string.section_saved_for_offline_new),
                selected = false,
                leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ArrowDownCircle),
                onClick = {
                    Analytics.tracker.trackEvent(OfflineChipButtonPressedEvent)
                    onNavigate(OfflineNavKey())
                },
            )
        }
        if (HomeChipSectionIds.CHAT !in hiddenSectionIds) {
            MegaChip(
                content = stringResource(sharedR.string.general_chat),
                selected = false,
                leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.MessageChatCircle),
                onClick = {
                    Analytics.tracker.trackEvent(ChatChipButtonPressedEvent)
                    onNavigate(ChatListNavKey())
                },
            )
        }

        Spacer(Modifier.size(8.dp))
    }
}

@CombinedThemePreviews
@Composable
private fun HomeChipsPreview() {
    AndroidThemeForPreviews {
        HomeChips(
            hiddenSectionIds = emptySet(),
            onNavigate = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun HomeChipsHiddenSectionsPreview() {
    AndroidThemeForPreviews {
        HomeChips(
            hiddenSectionIds = setOf(
                HomeChipSectionIds.FAVOURITES,
                HomeChipSectionIds.OFFLINE,
            ),
            onNavigate = {},
        )
    }
}
