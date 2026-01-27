package mega.privacy.android.feature.photos.presentation.handler

import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction
import mega.privacy.android.navigation.destination.LegacyAddToAlbumActivityNavKey
import mega.privacy.mobile.analytics.event.TimelineHideNodeMenuItemEvent

internal fun timelineActionsHandler(
    action: MenuActionWithIcon,
    selectedPhotosInTypedNode: () -> List<TypedNode>,
    actionHandler: (MenuAction, List<TypedNode>) -> Unit,
    onClearTimelinePhotosSelection: () -> Unit,
    onShowBottomSheet: () -> Unit,
    onNavigateToAddToAlbum: (key: LegacyAddToAlbumActivityNavKey) -> Unit,
) {
    when (action) {
        TimelineSelectionMenuAction.Download -> {
            actionHandler(
                DownloadMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.ShareLink -> {
            actionHandler(
                GetLinkMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.SendToChat -> {
            actionHandler(
                SendToChatMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.Share -> {
            actionHandler(
                ShareMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.MoveToRubbishBin -> {
            actionHandler(
                TrashMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.More -> {
            onShowBottomSheet()
        }

        TimelineSelectionMenuAction.RemoveLink -> {
            actionHandler(
                RemoveLinkMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.Hide -> {
            Analytics.tracker.trackEvent(TimelineHideNodeMenuItemEvent)
            actionHandler(
                HideMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.Unhide -> {
            actionHandler(
                UnhideMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.Move -> {
            actionHandler(
                MoveMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.Copy -> {
            actionHandler(
                CopyMenuAction(),
                selectedPhotosInTypedNode()
            )
            onClearTimelinePhotosSelection()
        }

        TimelineSelectionMenuAction.AddToAlbum -> {
            onNavigateToAddToAlbum(
                LegacyAddToAlbumActivityNavKey(
                    photoIds = selectedPhotosInTypedNode().map { it.id.longValue },
                    viewType = 0
                )
            )
            onClearTimelinePhotosSelection()
        }
    }
}
