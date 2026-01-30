package mega.privacy.android.feature.photos.extensions

import com.google.common.truth.Truth.assertThat
import java.util.stream.Stream
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.MediaScreenFilterMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenMoreMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenSearchMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenSettingsMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenSortByMenuToolbarEvent
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/** Test class for [MediaAppBarAction.toTrackingEvent] extension function */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaAppBarActionExtTest {

    @ParameterizedTest(name = "when action is {0}, returns {1}")
    @MethodSource("provideMappedActions")
    fun `test that mapped MediaAppBarAction returns correct tracking event`(
        action: MediaAppBarAction,
        expectedEvent: EventIdentifier,
    ) {
        val result = action.toTrackingEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @ParameterizedTest(name = "when action is Filter, returns null")
    @MethodSource("provideUnmappedActions")
    fun `test that unmapped MediaAppBarAction returns null`(
        action: MediaAppBarAction,
    ) {
        val result = action.toTrackingEvent()

        assertThat(result).isNull()
    }

    companion object {
        @JvmStatic
        fun provideMappedActions() =
            Stream.of(
                Arguments.of(MediaAppBarAction.Search, MediaScreenSearchMenuToolbarEvent),
                Arguments.of(MediaAppBarAction.More, MediaScreenMoreMenuToolbarEvent),
                Arguments.of(
                    MediaAppBarAction.FilterSecondary,
                    MediaScreenFilterMenuToolbarEvent
                ),
                Arguments.of(MediaAppBarAction.SortBy, MediaScreenSortByMenuToolbarEvent),
                Arguments.of(
                    MediaAppBarAction.CameraUploadsSettings,
                    MediaScreenSettingsMenuToolbarEvent
                ),
            )

        @JvmStatic
        fun provideUnmappedActions() =
            Stream.of(
                Arguments.of(MediaAppBarAction.Filter),
            )
    }
}
