package mega.privacy.android.feature.photos.extensions

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.MediaScreenAddToAlbumButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenCopyButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenDownloadButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenHideButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenLinkButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenMoveButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenRemoveLinkButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenRespondButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenShareButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenTrashButtonPressedEvent

internal fun MenuAction.toTrackingEvent(): EventIdentifier? = when (this) {
    is TimelineSelectionMenuAction.Download -> MediaScreenDownloadButtonPressedEvent
    is TimelineSelectionMenuAction.ShareLink -> MediaScreenLinkButtonPressedEvent
    is TimelineSelectionMenuAction.SendToChat -> MediaScreenRespondButtonPressedEvent
    is TimelineSelectionMenuAction.Share -> MediaScreenShareButtonPressedEvent
    is TimelineSelectionMenuAction.MoveToRubbishBin -> MediaScreenTrashButtonPressedEvent
    is TimelineSelectionMenuAction.More -> MediaScreenMoreButtonPressedEvent
    is TimelineSelectionMenuAction.AddToAlbum -> MediaScreenAddToAlbumButtonPressedEvent
    is TimelineSelectionMenuAction.Copy -> MediaScreenCopyButtonPressedEvent
    is TimelineSelectionMenuAction.Hide -> MediaScreenHideButtonPressedEvent
    is TimelineSelectionMenuAction.Move -> MediaScreenMoveButtonPressedEvent
    is TimelineSelectionMenuAction.RemoveLink -> MediaScreenRemoveLinkButtonPressedEvent
    else -> null
}
