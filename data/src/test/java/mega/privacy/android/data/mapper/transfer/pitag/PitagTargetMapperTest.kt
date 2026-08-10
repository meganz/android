package mega.privacy.android.data.mapper.transfer.pitag

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.pitag.PitagTargetMapper
import mega.privacy.android.domain.entity.pitag.PitagTarget
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PitagTargetMapperTest {

    private val underTest = PitagTargetMapper()

    @ParameterizedTest(name = " when PitagTarget is {0}")
    @MethodSource("provideParameters")
    fun `test that maps correctly`(
        pitagTarget: PitagTarget,
        expected: Char,
    ) {
        assertThat(underTest(pitagTarget)).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(PitagTarget.NotApplicable, MegaApiJava.PITAG_TARGET_NOT_APPLICABLE),
        Arguments.of(PitagTarget.CloudDrive, MegaApiJava.PITAG_TARGET_CLOUD_DRIVE),
        Arguments.of(PitagTarget.Chat1To1, MegaApiJava.PITAG_TARGET_CHAT_1TO1),
        Arguments.of(PitagTarget.ChatGroup, MegaApiJava.PITAG_TARGET_CHAT_GROUP),
        Arguments.of(PitagTarget.NoteToSelf, MegaApiJava.PITAG_TARGET_NOTE_TO_SELF),
        Arguments.of(PitagTarget.IncomingShare, MegaApiJava.PITAG_TARGET_INCOMING_SHARE),
        Arguments.of(PitagTarget.MultipleChats, MegaApiJava.PITAG_TARGET_MULTIPLE_CHATS),
    )
}