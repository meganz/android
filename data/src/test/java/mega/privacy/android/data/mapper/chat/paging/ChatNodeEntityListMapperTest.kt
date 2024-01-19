package mega.privacy.android.data.mapper.chat.paging

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ChatNodeEntityListMapperTest {
    private val underTest = ChatNodeEntityListMapper()

    @Test
    internal fun `test that fields are mapped correctly`() {
        val expectedId = NodeId(12L)
        val expectedMessageId = 432L
        val expectedName = "name"
        val expectedParentId = NodeId(765430L)
        val expectedBase64Id = "base64Id"
        val expectedLabel = 0
        val expectedIsFavourite = false
        val expectedExportedData = ExportedData(null, 1123L)
        val expectedIsTakenDown = false
        val expectedIsIncomingShare = false
        val expectedIsNodeKeyDecrypted = false
        val expectedCreationTime = 5432L
        val expectedSerializedData = "serializedData"
        val expectedIsAvailableOffline = false
        val expectedVersionCount = 0

        val node = mock<Node> {
            on { id } doReturn expectedId
            on { name } doReturn expectedName
            on { parentId } doReturn expectedParentId
            on { base64Id } doReturn expectedBase64Id
            on { label } doReturn expectedLabel
            on { isFavourite } doReturn expectedIsFavourite
            on { exportedData } doReturn expectedExportedData
            on { isTakenDown } doReturn expectedIsTakenDown
            on { isIncomingShare } doReturn expectedIsIncomingShare
            on { isNodeKeyDecrypted } doReturn expectedIsNodeKeyDecrypted
            on { creationTime } doReturn expectedCreationTime
            on { serializedData } doReturn expectedSerializedData
            on { isAvailableOffline } doReturn expectedIsAvailableOffline
            on { versionCount } doReturn expectedVersionCount
        }

        val actual = underTest(expectedMessageId, listOf(node))

        assertThat(actual).hasSize(1)
        val chatNodeEntity = actual[0]
        assertThat(chatNodeEntity.id).isEqualTo(expectedId)
        assertThat(chatNodeEntity.messageId).isEqualTo(expectedMessageId)
        assertThat(chatNodeEntity.name).isEqualTo(expectedName)
        assertThat(chatNodeEntity.parentId).isEqualTo(expectedParentId)
        assertThat(chatNodeEntity.base64Id).isEqualTo(expectedBase64Id)
        assertThat(chatNodeEntity.label).isEqualTo(expectedLabel)
        assertThat(chatNodeEntity.isFavourite).isEqualTo(expectedIsFavourite)
        assertThat(chatNodeEntity.exportedData).isEqualTo(expectedExportedData)
        assertThat(chatNodeEntity.isTakenDown).isEqualTo(expectedIsTakenDown)
        assertThat(chatNodeEntity.isIncomingShare).isEqualTo(expectedIsIncomingShare)
        assertThat(chatNodeEntity.isNodeKeyDecrypted).isEqualTo(expectedIsNodeKeyDecrypted)
        assertThat(chatNodeEntity.creationTime).isEqualTo(expectedCreationTime)
        assertThat(chatNodeEntity.serializedData).isEqualTo(expectedSerializedData)
        assertThat(chatNodeEntity.isAvailableOffline).isEqualTo(expectedIsAvailableOffline)
        assertThat(chatNodeEntity.versionCount).isEqualTo(expectedVersionCount)

    }
}