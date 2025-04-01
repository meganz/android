package mega.privacy.android.data.mapper.chat.messages

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll

/**
 * Test class for [PendingMessageMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PendingMessageMapperTest {

    private lateinit var underTest: PendingMessageMapper

    private val pendingMessageEntity = PendingMessageEntity(
        pendingMessageId = 123456L,
        transferUniqueId = 8745L,
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
    )

    @BeforeAll
    fun setUp() {
        underTest = PendingMessageMapper()
    }

    @Test
    fun `test that a pending message entity is mapped into a pending message`() = runTest {
        val pendingMessage = underTest(pendingMessageEntity)

        assertAll(
            "Grouped Assertions of ${PendingMessage::class.simpleName}",
            { assertThat(pendingMessage.id).isEqualTo(pendingMessageEntity.pendingMessageId) },
            { assertThat(pendingMessage.transferUniqueId).isEqualTo(pendingMessageEntity.transferUniqueId) },
            { assertThat(pendingMessage.chatId).isEqualTo(pendingMessageEntity.chatId) },
            { assertThat(pendingMessage.type).isEqualTo(pendingMessageEntity.type) },
            { assertThat(pendingMessage.uploadTimestamp).isEqualTo(pendingMessageEntity.uploadTimestamp) },
            { assertThat(pendingMessage.state).isEqualTo(pendingMessageEntity.state.value) },
            { assertThat(pendingMessage.tempIdKarere).isEqualTo(pendingMessageEntity.tempIdKarere) },
            { assertThat(pendingMessage.videoDownSampled).isEqualTo(pendingMessageEntity.videoDownSampled) },
            { assertThat(pendingMessage.uriPath).isEqualTo(UriPath(pendingMessageEntity.filePath)) },
            { assertThat(pendingMessage.nodeHandle).isEqualTo(pendingMessageEntity.nodeHandle) },
            { assertThat(pendingMessage.fingerprint).isEqualTo(pendingMessageEntity.fingerprint) },
            { assertThat(pendingMessage.name).isEqualTo(pendingMessageEntity.name) },
        )
    }

    @Test
    fun `test that a null pending message entity id is mapped to a -1 pending message id`() =
        runTest {
            val updatedPendingMessageEntity = pendingMessageEntity.copy(pendingMessageId = null)

            val pendingMessage = underTest(updatedPendingMessageEntity)

            assertThat(pendingMessage.id).isEqualTo(-1L)
        }
}