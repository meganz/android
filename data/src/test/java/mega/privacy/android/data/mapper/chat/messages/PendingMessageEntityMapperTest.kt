package mega.privacy.android.data.mapper.chat.messages

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [PendingMessageEntityMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PendingMessageEntityMapperTest {
    private lateinit var underTest: PendingMessageEntityMapper

    @BeforeAll
    fun setUp() {
        underTest = PendingMessageEntityMapper()
    }

    @Test
    fun `test that a saved pending message request is mapped into a pending message entity`() =
        runTest {
            val savePendingMessageRequest = SavePendingMessageRequest(
                chatId = 789012L,
                type = 5,
                uploadTimestamp = 11111L,
                state = PendingMessageState.COMPRESSING,
                tempIdKarere = 567890L,
                videoDownSampled = "videoDownSampled",
                filePath = "file/path",
                nodeHandle = 6543L,
                fingerprint = "903456L",
                name = "sample pending message",
                transferTag = 9,
            )
            val expected = PendingMessageEntity(
                chatId = savePendingMessageRequest.chatId,
                type = savePendingMessageRequest.type,
                uploadTimestamp = savePendingMessageRequest.uploadTimestamp,
                state = savePendingMessageRequest.state,
                tempIdKarere = savePendingMessageRequest.tempIdKarere,
                videoDownSampled = savePendingMessageRequest.videoDownSampled,
                filePath = savePendingMessageRequest.filePath,
                nodeHandle = savePendingMessageRequest.nodeHandle,
                fingerprint = savePendingMessageRequest.fingerprint,
                name = savePendingMessageRequest.name,
                transferTag = savePendingMessageRequest.transferTag,
            )

            val actual = underTest(savePendingMessageRequest)

            assertThat(actual).isEqualTo(expected)
        }
}