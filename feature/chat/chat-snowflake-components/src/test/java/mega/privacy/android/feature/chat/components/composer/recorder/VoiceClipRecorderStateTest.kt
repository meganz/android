package mega.privacy.android.feature.chat.components.composer.recorder

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class VoiceClipRecorderStateTest {

    private lateinit var timeSource: TestTimeSource
    private lateinit var underTest: VoiceClipRecorderState

    @BeforeEach
    fun setUp() {
        timeSource = TestTimeSource()
        underTest = VoiceClipRecorderState(
            lockThresholdPx = LOCK_THRESHOLD,
            cancelThresholdPx = CANCEL_THRESHOLD,
            timeSource = timeSource,
        )
    }

    @Test
    fun `test that start returns Start and sets recording when not recording`() {
        assertThat(underTest.start()).isEqualTo(VoiceClipRecordEvent.Start)
        assertThat(underTest.isRecording).isTrue()
        assertThat(underTest.isLocked).isFalse()
    }

    @Test
    fun `test that start returns null when already recording`() {
        underTest.start()

        assertThat(underTest.start()).isNull()
    }

    @Test
    fun `test that release returns Finish when the recording lasts the minimum duration`() {
        underTest.start()
        timeSource += 1.seconds

        assertThat(underTest.release()).isEqualTo(VoiceClipRecordEvent.Finish)
        assertThat(underTest.isRecording).isFalse()
        assertThat(underTest.showTapAndHoldHint).isFalse()
    }

    @Test
    fun `test that release returns Cancel and shows the hint when the recording is too short`() {
        underTest.start()
        timeSource += 999.milliseconds

        assertThat(underTest.release()).isEqualTo(VoiceClipRecordEvent.Cancel)
        assertThat(underTest.isRecording).isFalse()
        assertThat(underTest.showTapAndHoldHint).isTrue()
    }

    @Test
    fun `test that release returns null when not recording`() {
        assertThat(underTest.release()).isNull()
    }

    @Test
    fun `test that start hides the hint when the previous recording was too short`() {
        underTest.start()
        underTest.release()

        underTest.start()

        assertThat(underTest.showTapAndHoldHint).isFalse()
    }

    @Test
    fun `test that drag keeps only the horizontal offset when the finger moves more towards the start`() {
        underTest.start()

        assertThat(underTest.drag(Offset(-50f, -20f))).isNull()
        assertThat(underTest.dragOffset).isEqualTo(Offset(-50f, 0f))
    }

    @Test
    fun `test that drag keeps only the vertical offset when the finger moves more up`() {
        underTest.start()

        assertThat(underTest.drag(Offset(-20f, -50f))).isNull()
        assertThat(underTest.dragOffset).isEqualTo(Offset(0f, -50f))
    }

    @Test
    fun `test that drag ignores movement towards the end and down`() {
        underTest.start()

        underTest.drag(Offset(80f, 90f))

        assertThat(underTest.dragOffset).isEqualTo(Offset.Zero)
    }

    @Test
    fun `test that drag returns Cancel and stops recording when the finger passes the cancel threshold`() {
        underTest.start()

        assertThat(underTest.drag(Offset(-(CANCEL_THRESHOLD + 1), 0f)))
            .isEqualTo(VoiceClipRecordEvent.Cancel)
        assertThat(underTest.isRecording).isFalse()
        assertThat(underTest.dragOffset).isEqualTo(Offset.Zero)
    }

    @Test
    fun `test that drag returns Lock and locks the recording when the finger passes the lock threshold`() {
        underTest.start()

        assertThat(underTest.drag(Offset(0f, -(LOCK_THRESHOLD + 1))))
            .isEqualTo(VoiceClipRecordEvent.Lock)
        assertThat(underTest.isRecording).isTrue()
        assertThat(underTest.isLocked).isTrue()
        assertThat(underTest.dragOffset).isEqualTo(Offset.Zero)
    }

    @Test
    fun `test that drag returns null when the recording is locked`() {
        underTest.start()
        underTest.drag(Offset(0f, -(LOCK_THRESHOLD + 1)))

        assertThat(underTest.drag(Offset(-(CANCEL_THRESHOLD + 1), 0f))).isNull()
        assertThat(underTest.isRecording).isTrue()
    }

    @Test
    fun `test that release returns null when the recording is locked`() {
        underTest.start()
        underTest.drag(Offset(0f, -(LOCK_THRESHOLD + 1)))

        assertThat(underTest.release()).isNull()
        assertThat(underTest.isRecording).isTrue()
    }

    @Test
    fun `test that cancel returns Cancel and stops recording when the recording is locked`() {
        underTest.start()
        underTest.drag(Offset(0f, -(LOCK_THRESHOLD + 1)))

        assertThat(underTest.cancel()).isEqualTo(VoiceClipRecordEvent.Cancel)
        assertThat(underTest.isRecording).isFalse()
        assertThat(underTest.isLocked).isFalse()
    }

    @Test
    fun `test that finish returns Finish and stops recording when the recording is locked`() {
        underTest.start()
        underTest.drag(Offset(0f, -(LOCK_THRESHOLD + 1)))

        assertThat(underTest.finish()).isEqualTo(VoiceClipRecordEvent.Finish)
        assertThat(underTest.isRecording).isFalse()
    }

    @Test
    fun `test that cancel and finish return null when not recording`() {
        assertThat(underTest.cancel()).isNull()
        assertThat(underTest.finish()).isNull()
    }

    @Test
    fun `test that elapsed returns the time since start when recording`() {
        underTest.start()
        timeSource += 75.seconds

        assertThat(underTest.elapsed).isEqualTo(75.seconds)
    }

    @Test
    fun `test that elapsed returns zero when not recording`() {
        underTest.start()
        timeSource += 5.seconds
        underTest.cancel()

        assertThat(underTest.elapsed).isEqualTo(0.seconds)
    }

    @Test
    fun `test that formatRecordingTime pads minutes and seconds to two digits`() {
        assertThat(5L.formatRecordingTime()).isEqualTo("00:05")
        assertThat(75L.formatRecordingTime()).isEqualTo("01:15")
        assertThat(3600L.formatRecordingTime()).isEqualTo("60:00")
    }

    companion object {
        private const val LOCK_THRESHOLD = 100f
        private const val CANCEL_THRESHOLD = 300f
    }
}
