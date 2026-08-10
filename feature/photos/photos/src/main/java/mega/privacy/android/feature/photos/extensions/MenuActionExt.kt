package mega.privacy.android.feature.photos.extensions

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.AddToAlbumMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
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

internal fun MenuActionWithIcon.toTrackingEvent(): EventIdentifier? = when (this) {
    is DownloadMenuAction -> MediaScreenDownloadButtonPressedEvent
    is GetLinkMenuAction -> MediaScreenLinkButtonPressedEvent
    is SendToChatMenuAction -> MediaScreenRespondButtonPressedEvent
    is ShareMenuAction -> MediaScreenShareButtonPressedEvent
    is TrashMenuAction -> MediaScreenTrashButtonPressedEvent
    is CommonMenuAction.More -> MediaScreenMoreButtonPressedEvent
    is AddToAlbumMenuAction -> MediaScreenAddToAlbumButtonPressedEvent
    is CopyMenuAction -> MediaScreenCopyButtonPressedEvent
    is HideMenuAction -> MediaScreenHideButtonPressedEvent
    is MoveMenuAction -> MediaScreenMoveButtonPressedEvent
    is RemoveLinkMenuAction -> MediaScreenRemoveLinkButtonPressedEvent
    else -> null
}
