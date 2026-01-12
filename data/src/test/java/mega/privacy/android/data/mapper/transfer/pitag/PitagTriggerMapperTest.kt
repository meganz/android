package mega.privacy.android.data.mapper.transfer.pitag

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.pitag.PitagTriggerMapper
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PitagTriggerMapperTest {

    private val underTest = PitagTriggerMapper()

    @ParameterizedTest(name = " when PitagTrigger is {0}")
    @MethodSource("provideParameters")
    fun `test that maps correctly`(
        pitagTrigger: PitagTrigger,
        expected: Char,
    ) {
        assertThat(underTest(pitagTrigger)).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(PitagTrigger.NotApplicable, MegaApiJava.PITAG_TRIGGER_NOT_APPLICABLE),
        Arguments.of(PitagTrigger.Picker, MegaApiJava.PITAG_TRIGGER_PICKER),
        Arguments.of(PitagTrigger.DragAndDrop, MegaApiJava.PITAG_TRIGGER_DRAG_AND_DROP),
        Arguments.of(PitagTrigger.Camera, MegaApiJava.PITAG_TRIGGER_CAMERA),
        Arguments.of(PitagTrigger.Scanner, MegaApiJava.PITAG_TRIGGER_SCANNER),
        Arguments.of(PitagTrigger.SyncAlgorithm, MegaApiJava.PITAG_TRIGGER_SYNC_ALGORITHM),
    )
}