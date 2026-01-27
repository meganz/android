package mega.privacy.android.feature.photos.extensions

import com.google.common.truth.Truth.assertThat
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
        action: TimelineSelectionMenuAction,
        expectedEvent: EventIdentifier,
    ) {
        val result = action.toTrackingEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @ParameterizedTest(name = "when action is {0}, returns null")
    @MethodSource("provideUnmappedActions")
    fun `test that unmapped actions return null`(
        action: TimelineSelectionMenuAction,
    ) {
        val result = action.toTrackingEvent()

        assertThat(result).isNull()
    }

    private fun provideMappedActions() = Stream.of(
        Arguments.of(
            TimelineSelectionMenuAction.Download,
            MediaScreenDownloadButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.ShareLink,
            MediaScreenLinkButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.SendToChat,
            MediaScreenRespondButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.Share,
            MediaScreenShareButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.MoveToRubbishBin,
            MediaScreenTrashButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.More,
            MediaScreenMoreButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.AddToAlbum,
            MediaScreenAddToAlbumButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.Copy,
            MediaScreenCopyButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.Hide,
            MediaScreenHideButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.Move,
            MediaScreenMoveButtonPressedEvent
        ),
        Arguments.of(
            TimelineSelectionMenuAction.RemoveLink,
            MediaScreenRemoveLinkButtonPressedEvent
        ),
    )

    private fun provideUnmappedActions() = Stream.of(
        Arguments.of(TimelineSelectionMenuAction.Unhide),
    )
}
