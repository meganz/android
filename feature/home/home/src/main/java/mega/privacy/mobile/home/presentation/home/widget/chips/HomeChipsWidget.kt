package mega.privacy.mobile.home.presentation.home.widget.chips

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.FavouritesNavKey
import mega.privacy.android.navigation.destination.LegacyAudioSectionNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.navigation.destination.VideoSectionNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AudiosChipButtonPressedEvent
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
        val viewModel = hiltViewModel<HomeChipsWidgetViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        HomeChips(
            isVideosChipVisible = !uiState.isMediaRevampPhase2Enabled,
            isAudiosChipVisible = uiState.isAudiosChipVisible,
            modifier = modifier,
            onNavigate = navigationHandler::navigate
        )
    }
}

@Composable
private fun HomeChips(
    isVideosChipVisible: Boolean,
    isAudiosChipVisible: Boolean,
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
            content = stringResource(sharedR.string.video_section_title_favourite_playlist),
            selected = false,
            leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.Heart),
            onClick = {
                Analytics.tracker.trackEvent(FavouritesChipButtonPressedEvent)
                onNavigate(FavouritesNavKey)
            },
        )
        if (isVideosChipVisible) {
            MegaChip(
                content = stringResource(sharedR.string.media_videos_tab_title),
                selected = false,
                leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.Film),
                onClick = {
                    Analytics.tracker.trackEvent(VideosChipButtonPressedEvent)
                    onNavigate(VideoSectionNavKey)
                },
            )
        }
        if (isAudiosChipVisible) {
            MegaChip(
                content = stringResource(sharedR.string.home_screen_audios_chip_title),
                selected = false,
                leadingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.Music),
                onClick = {
                    Analytics.tracker.trackEvent(AudiosChipButtonPressedEvent)
                    onNavigate(LegacyAudioSectionNavKey)
                },
            )
        }
        MegaChip(
            content = stringResource(sharedR.string.section_saved_for_offline_new),
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
        LazyColumn {
            listOf(false, true).forEach {
                item {
                    HomeChips(
                        isVideosChipVisible = it,
                        isAudiosChipVisible = it,
                        onNavigate = {}
                    )
                }
            }
        }
    }
}