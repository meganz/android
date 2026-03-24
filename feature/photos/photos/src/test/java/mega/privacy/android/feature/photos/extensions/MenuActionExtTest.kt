package mega.privacy.android.feature.photos.extensions

import com.google.common.truth.Truth.assertThat
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
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.shared.nodes.model.NodeSelectionAction
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [MenuAction.toTrackingEvent] extension function
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MenuActionExtTest {

    @ParameterizedTest(name = "when action is {0}, returns {1}")
    @MethodSource("provideMappedActions")
    fun `test that mapped actions return correct tracking events`(
        action: MenuActionWithIcon,
        expectedEvent: EventIdentifier,
    ) {
        val result = action.toTrackingEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @ParameterizedTest(name = "when action is {0}, returns null")
    @MethodSource("provideUnmappedActions")
    fun `test that unmapped actions return null`(
        action: MenuActionWithIcon,
    ) {
        val result = action.toTrackingEvent()

        assertThat(result).isNull()
    }

    private fun provideMappedActions() = Stream.of(
        Arguments.of(
            DownloadMenuAction(),
            MediaScreenDownloadButtonPressedEvent
        ),
        Arguments.of(
            GetLinkMenuAction(),
            MediaScreenLinkButtonPressedEvent
        ),
        Arguments.of(
            SendToChatMenuAction(),
            MediaScreenRespondButtonPressedEvent
        ),
        Arguments.of(
            ShareMenuAction(),
            MediaScreenShareButtonPressedEvent
        ),
        Arguments.of(
            TrashMenuAction(),
            MediaScreenTrashButtonPressedEvent
        ),
        Arguments.of(
            NodeSelectionAction.More,
            MediaScreenMoreButtonPressedEvent
        ),
        Arguments.of(
            AddToAlbumMenuAction(),
            MediaScreenAddToAlbumButtonPressedEvent
        ),
        Arguments.of(
            CopyMenuAction(),
            MediaScreenCopyButtonPressedEvent
        ),
        Arguments.of(
            HideMenuAction(),
            MediaScreenHideButtonPressedEvent
        ),
        Arguments.of(
            MoveMenuAction(),
            MediaScreenMoveButtonPressedEvent
        ),
        Arguments.of(
            RemoveLinkMenuAction(),
            MediaScreenRemoveLinkButtonPressedEvent
        ),
    )

    private fun provideUnmappedActions() = Stream.of(
        Arguments.of(UnhideMenuAction()),
    )
}
