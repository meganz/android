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
            on { changes }.thenReturn(MegaChatSession.CHANGE_TYPE_PERMISSIONS)
            on { isAudioDetected }.thenReturn(true)
            on { clientid }.thenReturn(12345L)
            on { isOnHold }.thenReturn(true)
            on { status }.thenReturn(MegaChatSession.SESSION_STATUS_IN_PROGRESS)
            on { isHiResCamera }.thenReturn(true)
            on { isHiResScreenShare }.thenReturn(true)
            on { isHiResVideo }.thenReturn(true)
            on { isLowResCamera }.thenReturn(false)
            on { isLowResScreenShare }.thenReturn(false)
            on { isLowResVideo }.thenReturn(false)
            on { isModerator }.thenReturn(true)
            on { peerid }.thenReturn(98765L)
            on { termCode }.thenReturn(MegaChatSession.SESS_TERM_CODE_RECOVERABLE)
        }

        val mappedData = underTest(megaChatSession)

        Truth.assertThat(mappedData.changes).isEqualTo(ChatSessionChanges.Permissions)
        Truth.assertThat(mappedData.isAudioDetected).isEqualTo(true)
        Truth.assertThat(mappedData.clientId).isEqualTo(12345L)
        Truth.assertThat(mappedData.isOnHold).isEqualTo(true)
        Truth.assertThat(mappedData.status).isEqualTo(ChatSessionStatus.Progress)
        Truth.assertThat(mappedData.isHiResCamera).isEqualTo(true)
        Truth.assertThat(mappedData.isHiResVideo).isEqualTo(true)
        Truth.assertThat(mappedData.isHiResScreenShare).isEqualTo(true)
        Truth.assertThat(mappedData.isLowResCamera).isEqualTo(false)
        Truth.assertThat(mappedData.isLowResVideo).isEqualTo(false)
        Truth.assertThat(mappedData.isLowResScreenShare).isEqualTo(false)
        Truth.assertThat(mappedData.isModerator).isEqualTo(true)
        Truth.assertThat(mappedData.peerId).isEqualTo(98765L)
        Truth.assertThat(mappedData.termCode).isEqualTo(ChatSessionTermCode.Recoverable)
    }


}