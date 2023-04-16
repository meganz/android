package mega.privacy.android.domain.usecase.mediaplayer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.statistics.MediaPlayerStatisticsEvents
import mega.privacy.android.domain.repository.StatisticsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import java.util.stream.Stream

/**
 * Test class for [SendStatisticsMediaPlayerUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendStatisticsMediaPlayerUseCaseTest {

    private lateinit var underTest: SendStatisticsMediaPlayerUseCase

    private val statisticsRepository = Mockito.mock<StatisticsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SendStatisticsMediaPlayerUseCase(
            statisticsRepository = statisticsRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(statisticsRepository)
    }

    @ParameterizedTest(name = "test that send event for \"{0}\" statistics is fired")
    @MethodSource("provideParameters")
    fun `test that send event for media player statistics is fired`(
        name: String,
        event: MediaPlayerStatisticsEvents,
    ) {
        runTest {
            underTest(event)
            verify(statisticsRepository).sendEvent(event.id, event.message)
        }
    }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(
                    MediaPlayerStatisticsEvents.VideoPlayerActivatedEvent().message,
                    MediaPlayerStatisticsEvents.VideoPlayerActivatedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.SubtitleDialogShownEvent().message,
                    MediaPlayerStatisticsEvents.SubtitleDialogShownEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.HideSubtitleEvent().message,
                    MediaPlayerStatisticsEvents.HideSubtitleEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.AutoMatchSubtitleClickedEvent().message,
                    MediaPlayerStatisticsEvents.AutoMatchSubtitleClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.OpenSelectSubtitlePageEvent().message,
                    MediaPlayerStatisticsEvents.OpenSelectSubtitlePageEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.SearchModeEnabledEvent().message,
                    MediaPlayerStatisticsEvents.SearchModeEnabledEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.AddSubtitleClickedEvent().message,
                    MediaPlayerStatisticsEvents.AddSubtitleClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.SelectSubtitleCancelledEvent().message,
                    MediaPlayerStatisticsEvents.SelectSubtitleCancelledEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.LoopButtonEnabledEvent().message,
                    MediaPlayerStatisticsEvents.LoopButtonEnabledEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.ScreenLockedEvent().message,
                    MediaPlayerStatisticsEvents.ScreenLockedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.ScreenUnlockedEvent().message,
                    MediaPlayerStatisticsEvents.ScreenUnlockedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.SnapshotButtonClickedEvent().message,
                    MediaPlayerStatisticsEvents.SnapshotButtonClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.InfoButtonClickedEvent().message,
                    MediaPlayerStatisticsEvents.InfoButtonClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.SaveToDeviceButtonClickedEvent().message,
                    MediaPlayerStatisticsEvents.SaveToDeviceButtonClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.SendToChatButtonClickedEvent().message,
                    MediaPlayerStatisticsEvents.SendToChatButtonClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.ShareButtonClickedEvent().message,
                    MediaPlayerStatisticsEvents.ShareButtonClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.GetLinkButtonClickedEvent().message,
                    MediaPlayerStatisticsEvents.GetLinkButtonClickedEvent()
                ),
                Arguments.of(
                    MediaPlayerStatisticsEvents.RemoveLinkButtonClickedEvent().message,
                    MediaPlayerStatisticsEvents.RemoveLinkButtonClickedEvent()
                )
            )
        }
    }
}