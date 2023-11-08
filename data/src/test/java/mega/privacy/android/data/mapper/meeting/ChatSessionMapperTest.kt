package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import mega.privacy.android.domain.entity.meeting.ChatSessionStatus
import mega.privacy.android.domain.entity.meeting.ChatSessionTermCode
import nz.mega.sdk.MegaChatSession
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatSessionMapperTest {
    private val chatSessionChangesMapper = ChatSessionChangesMapper()
    private val chatSessionTermCodeMapper = ChatSessionTermCodeMapper()
    private val chatSessionStatusMapper = ChatSessionStatusMapper()
    private val underTest: ChatSessionMapper = ChatSessionMapper(
        chatSessionChangesMapper, chatSessionStatusMapper, chatSessionTermCodeMapper
    )

    @Test
    fun `test that sample mega chat session is mapped correctly`() {
        val megaChatSession = mock<MegaChatSession> {
            on { status }.thenReturn(MegaChatSession.SESSION_STATUS_IN_PROGRESS)
            on { peerid }.thenReturn(98765L)
            on { clientid }.thenReturn(12345L)
            on { isSpeakAllowed }.thenReturn(true)
            on { hasAudio() }.thenReturn(true)
            on { hasVideo() }.thenReturn(true)
            on { isHiResVideo }.thenReturn(true)
            on { isLowResVideo }.thenReturn(false)
            on { hasCamera() }.thenReturn(true)
            on { isLowResCamera }.thenReturn(false)
            on { isHiResCamera }.thenReturn(true)
            on { hasScreenShare() }.thenReturn(true)
            on { isHiResScreenShare }.thenReturn(true)
            on { isLowResScreenShare }.thenReturn(false)
            on { isOnHold }.thenReturn(true)
            on { changes }.thenReturn(MegaChatSession.CHANGE_TYPE_PERMISSIONS)
            on { termCode }.thenReturn(MegaChatSession.SESS_TERM_CODE_RECOVERABLE)
            on { hasPendingSpeakRequest() }.thenReturn(false)
            on { isAudioDetected }.thenReturn(true)
            on { canRecvVideoHiRes() }.thenReturn(true)
            on { canRecvVideoLowRes() }.thenReturn(false)
            on { isModerator }.thenReturn(true)
            on { isRecording }.thenReturn(true)
            on { hasSpeakPermission() }.thenReturn(false)
        }

        val mappedData = underTest(megaChatSession)

        Truth.assertThat(mappedData.status).isEqualTo(ChatSessionStatus.Progress)
        Truth.assertThat(mappedData.peerId).isEqualTo(98765L)
        Truth.assertThat(mappedData.clientId).isEqualTo(12345L)
        Truth.assertThat(mappedData.isSpeakAllowed).isEqualTo(true)
        Truth.assertThat(mappedData.hasAudio).isEqualTo(true)
        Truth.assertThat(mappedData.hasVideo).isEqualTo(true)
        Truth.assertThat(mappedData.isHiResVideo).isEqualTo(true)
        Truth.assertThat(mappedData.isLowResVideo).isEqualTo(false)
        Truth.assertThat(mappedData.hasCamera).isEqualTo(true)
        Truth.assertThat(mappedData.isLowResCamera).isEqualTo(false)
        Truth.assertThat(mappedData.isHiResCamera).isEqualTo(true)
        Truth.assertThat(mappedData.hasScreenShare).isEqualTo(true)
        Truth.assertThat(mappedData.isHiResScreenShare).isEqualTo(true)
        Truth.assertThat(mappedData.isLowResScreenShare).isEqualTo(false)
        Truth.assertThat(mappedData.isOnHold).isEqualTo(true)
        Truth.assertThat(mappedData.changes).isEqualTo(ChatSessionChanges.Permissions)
        Truth.assertThat(mappedData.termCode).isEqualTo(ChatSessionTermCode.Recoverable)
        Truth.assertThat(mappedData.hasPendingSpeakRequest).isEqualTo(false)
        Truth.assertThat(mappedData.isAudioDetected).isEqualTo(true)
        Truth.assertThat(mappedData.canReceiveVideoHiRes).isEqualTo(true)
        Truth.assertThat(mappedData.canReceiveVideoLowRes).isEqualTo(false)
        Truth.assertThat(mappedData.isModerator).isEqualTo(true)
        Truth.assertThat(mappedData.isRecording).isEqualTo(true)
        Truth.assertThat(mappedData.hasSpeakPermission).isEqualTo(false)
    }
}