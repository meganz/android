package mega.privacy.mobile.home.presentation.home.widget.chips

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.FavouritesNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.navigation.destination.VideoSectionNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ChatChipButtonPressedEvent
import mega.privacy.mobile.analytics.event.FavouritesChipButtonPressedEvent
import mega.privacy.mobile.analytics.event.OfflineChipButtonPressedEvent
import mega.privacy.mobile.analytics.event.VideosChipButtonPressedEvent
import javax.inject.Inject

class HomeChipsWidget @Inject constructor(
) : HomeWidget {

    override val identifier: String = "HomeChipsWidgetProvider"
    override val defaultOrder: Int = 0
    override val canDelete: Boolean = false

    override suspend fun getWidgetName() = LocalizedText.Literal("Home Chips")

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        HomeChips(modifier = modifier, onNavigate = navigationHandler::navigate)
    }
}

@Composable
private fun HomeChips(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.size(8.dp)) // Total 16dp with spacedBy

        MegaChip(
            content = stringResource(R.string.favourites_category_title),
            selected = false,
            leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.Heart),
            onClick = {
                Analytics.tracker.trackEvent(FavouritesChipButtonPressedEvent)
                onNavigate(FavouritesNavKey)
            },
        )
        MegaChip(
            content = stringResource(sharedR.string.media_videos_tab_title),
            selected = false,
            leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.Film),
            onClick = {
                Analytics.tracker.trackEvent(VideosChipButtonPressedEvent)
                onNavigate(VideoSectionNavKey)
            },
        )
        MegaChip(
            content = stringResource(R.string.section_saved_for_offline_new),
            selected = false,
            leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ArrowDownCircle),
            onClick = {
                Analytics.tracker.trackEvent(OfflineChipButtonPressedEvent)
                onNavigate(OfflineNavKey())
            },
        )
        MegaChip(
            content = stringResource(sharedR.string.general_chat),
            selected = false,
            leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.MessageChatCircle),
            onClick = {
                Analytics.tracker.trackEvent(ChatChipButtonPressedEvent)
                onNavigate(ChatListNavKey())
            },
        )

        Spacer(Modifier.size(8.dp))
    }
}

@CombinedThemePreviews
@Composable
private fun HomeChipsPreview() {
    AndroidThemeForPreviews {
        HomeChips(
            onNavigate = {}
        )
    }
}