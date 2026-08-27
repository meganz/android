package mega.privacy.mobile.home.presentation.home.widget.domore

import com.google.common.truth.Truth.assertThat
import mega.privacy.mobile.analytics.core.event.identifier.ButtonPressedEventIdentifier
import mega.privacy.mobile.analytics.event.DoMoreWithMegaAddContactButtonPressedEvent
import mega.privacy.mobile.analytics.event.DoMoreWithMegaAddSyncButtonPressedEvent
import mega.privacy.mobile.analytics.event.DoMoreWithMegaCameraUploadsButtonPressedEvent
import mega.privacy.mobile.analytics.event.DoMoreWithMegaCreateAlbumButtonPressedEvent
import mega.privacy.mobile.analytics.event.DoMoreWithMegaScanDocumentButtonPressedEvent
import mega.privacy.mobile.analytics.event.DoMoreWithMegaScheduleMeetingButtonPressedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoMoreWithMegaWidgetTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideIdentifierEvents")
    fun `test that buttonPressedEvent returns the matching event for each shortcut`(
        identifier: DoMoreWithMegaItem.Identifier,
        expected: ButtonPressedEventIdentifier,
    ) {
        assertThat(identifier.buttonPressedEvent()).isEqualTo(expected)
    }

    @Test
    fun `test that buttonPressedEvent returns a distinct event for every shortcut`() {
        val events = DoMoreWithMegaItem.Identifier.entries.map { it.buttonPressedEvent() }

        assertThat(events.toSet()).hasSize(DoMoreWithMegaItem.Identifier.entries.size)
    }

    private fun provideIdentifierEvents(): Stream<Arguments> = Stream.of(
        Arguments.of(
            DoMoreWithMegaItem.Identifier.CameraUploads,
            DoMoreWithMegaCameraUploadsButtonPressedEvent,
        ),
        Arguments.of(
            DoMoreWithMegaItem.Identifier.AddSync,
            DoMoreWithMegaAddSyncButtonPressedEvent,
        ),
        Arguments.of(
            DoMoreWithMegaItem.Identifier.ScanDocument,
            DoMoreWithMegaScanDocumentButtonPressedEvent,
        ),
        Arguments.of(
            DoMoreWithMegaItem.Identifier.CreateAlbum,
            DoMoreWithMegaCreateAlbumButtonPressedEvent,
        ),
        Arguments.of(
            DoMoreWithMegaItem.Identifier.AddContact,
            DoMoreWithMegaAddContactButtonPressedEvent,
        ),
        Arguments.of(
            DoMoreWithMegaItem.Identifier.ScheduleMeeting,
            DoMoreWithMegaScheduleMeetingButtonPressedEvent,
        ),
    )
}
