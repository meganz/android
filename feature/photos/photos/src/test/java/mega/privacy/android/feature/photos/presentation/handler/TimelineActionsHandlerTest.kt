package mega.privacy.android.feature.photos.presentation.handler

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.destination.LegacyAddToAlbumActivityNavKey
import mega.privacy.mobile.analytics.event.TimelineHideNodeMenuItemEvent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineActionsHandlerTest {

    private val analyticsTracker: AnalyticsTracker = mock()

    @BeforeAll
    fun setup() {
        Analytics.initialise(analyticsTracker)
    }

    @Test
    fun `test that the download menu action is successfully handled and the selection is successfully cleared when the action is for download`() {
        val action = TimelineSelectionMenuAction.Download
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(DownloadMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the get link menu action is successfully handled and the selection is successfully cleared when the action is for share link`() {
        val action = TimelineSelectionMenuAction.ShareLink
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(GetLinkMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the send to chat menu action is successfully handled and the selection is successfully cleared when the action is for send to chat`() {
        val action = TimelineSelectionMenuAction.SendToChat
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(SendToChatMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the share menu action is successfully handled and the selection is successfully cleared when the action is for share`() {
        val action = TimelineSelectionMenuAction.Share
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(ShareMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the trash menu action is successfully handled and the selection is successfully cleared when the action is for move to rubbish bin`() {
        val action = TimelineSelectionMenuAction.MoveToRubbishBin
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(TrashMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the more bottom sheet is displayed when the action more is clicked`() {
        val action = TimelineSelectionMenuAction.More
        val onShowBottomSheet = mock<() -> Unit>()

        callHandler(
            action = action,
            onShowBottomSheet = onShowBottomSheet
        )

        verify(onShowBottomSheet).invoke()
    }

    @Test
    fun `test that the remove link menu action is successfully handled and the selection is successfully cleared when the action is for remove link`() {
        val action = TimelineSelectionMenuAction.RemoveLink
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(RemoveLinkMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the hide menu action is successfully handled and the selection is successfully cleared when the action is for hide`() {
        val action = TimelineSelectionMenuAction.Hide
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(HideMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the hide menu action is successfully tracked`() {
        val action = TimelineSelectionMenuAction.Hide

        callHandler(action = action)

        verify(analyticsTracker).trackEvent(TimelineHideNodeMenuItemEvent)
    }

    @Test
    fun `test that the unhide menu action is successfully handled and the selection is successfully cleared when the action is for unhide`() {
        val action = TimelineSelectionMenuAction.Unhide
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(UnhideMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the move menu action is successfully handled and the selection is successfully cleared when the action is for move`() {
        val action = TimelineSelectionMenuAction.Move
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(MoveMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the copy menu action is successfully handled and the selection is successfully cleared when the action is for copy`() {
        val action = TimelineSelectionMenuAction.Copy
        val actionHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val selectedPhotos = mock<List<TypedNode>>()
        whenever(selectedPhotosInTypedNode()) doReturn selectedPhotos

        callHandler(
            action = action,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode
        )

        val menuAction = argumentCaptor<MenuAction>()
        val selectedPhotosCaptor = argumentCaptor<List<TypedNode>>()
        verify(actionHandler).invoke(
            menuAction.capture(),
            selectedPhotosCaptor.capture()
        )
        assertThat(menuAction.firstValue).isInstanceOf(CopyMenuAction::class.java)
        assertThat(selectedPhotosCaptor.firstValue).isEqualTo(selectedPhotos)
        verify(onClearTimelinePhotosSelection).invoke()
    }

    @Test
    fun `test that the user is navigated to add to album screen and the selection is successfully cleared when the selected menu action is for add to album`() {
        val action = TimelineSelectionMenuAction.AddToAlbum
        val selectedPhotosInTypedNode = mock<() -> List<TypedNode>>()
        val photoId = NodeId(longValue = 123L)
        val photo = mock<TypedNode> {
            on { id } doReturn photoId
        }
        whenever(selectedPhotosInTypedNode()) doReturn listOf(photo)
        val onClearTimelinePhotosSelection = mock<() -> Unit>()
        val onNavigateToAddToAlbum = mock<(key: LegacyAddToAlbumActivityNavKey) -> Unit>()

        callHandler(
            action = action,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            onNavigateToAddToAlbum = onNavigateToAddToAlbum
        )

        val captor = argumentCaptor<LegacyAddToAlbumActivityNavKey>()
        verify(onNavigateToAddToAlbum).invoke(captor.capture())
        assertThat(captor.firstValue).isEqualTo(
            LegacyAddToAlbumActivityNavKey(
                photoIds = listOf(photoId.longValue),
                viewType = 0
            )
        )
        verify(onClearTimelinePhotosSelection).invoke()
    }

    private fun callHandler(
        action: MenuActionWithIcon = object : MenuActionWithIcon {
            @Composable
            override fun getIconPainter(): Painter =
                rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchLarge)

            override val testTag: String = ""

            @Composable
            override fun getDescription(): String = ""
        },
        selectedPhotosInTypedNode: () -> List<TypedNode> = { emptyList() },
        actionHandler: (MenuAction, List<TypedNode>) -> Unit = { _, _ -> },
        onClearTimelinePhotosSelection: () -> Unit = {},
        onShowBottomSheet: () -> Unit = {},
        onNavigateToAddToAlbum: (key: LegacyAddToAlbumActivityNavKey) -> Unit = {},
    ) {
        timelineActionsHandler(
            action = action,
            selectedPhotosInTypedNode = selectedPhotosInTypedNode,
            actionHandler = actionHandler,
            onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
            onShowBottomSheet = onShowBottomSheet,
            onNavigateToAddToAlbum = onNavigateToAddToAlbum
        )
    }
}
