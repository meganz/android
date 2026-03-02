package mega.privacy.android.feature.photos.presentation.timeline.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import java.time.ZoneId

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotoToTypedNodeMapperTest {

    private lateinit var underTest: PhotoToTypedNodeMapper

    @BeforeEach
    fun setup() {
        underTest = PhotoToTypedNodeMapper()
    }

    @Test
    fun `test that the correct mapped TypedNode is returned`() {
        val time = LocalDateTime.now()
        val photo = mock<Photo.Image> {
            on { id } doReturn 1L
            on { name } doReturn "name"
            on { parentId } doReturn 2L
            on { base64Id } doReturn "base64Id"
            on { restoreId } doReturn NodeId(longValue = 3L)
            on { label } doReturn 1
            on { nodeLabel } doReturn NodeLabel.GREEN
            on { isFavourite } doReturn true
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { exportedData } doReturn null
            on { isTakenDown } doReturn false
            on { isIncomingShare } doReturn true
            on { isNodeKeyDecrypted } doReturn true
            on { creationTime } doReturn time
            on { serializedData } doReturn "serializedData"
            on { isAvailableOffline } doReturn true
            on { versionCount } doReturn 12
            on { description } doReturn "description"
            on { tags } doReturn listOf("tags")
        }

        val actual = underTest(photo = photo)

        assertThat(actual.id).isEqualTo(NodeId(longValue = photo.id))
        assertThat(actual.name).isEqualTo(photo.name)
        assertThat(actual.parentId).isEqualTo(NodeId(longValue = photo.parentId))
        assertThat(actual.base64Id).isEqualTo(photo.base64Id)
        assertThat(actual.restoreId).isEqualTo(photo.restoreId)
        assertThat(actual.label).isEqualTo(photo.label)
        assertThat(actual.nodeLabel).isEqualTo(photo.nodeLabel)
        assertThat(actual.isFavourite).isEqualTo(photo.isFavourite)
        assertThat(actual.isMarkedSensitive).isEqualTo(photo.isSensitive)
        assertThat(actual.isSensitiveInherited).isEqualTo(photo.isSensitiveInherited)
        assertThat(actual.exportedData).isEqualTo(photo.exportedData)
        assertThat(actual.isTakenDown).isEqualTo(photo.isTakenDown)
        assertThat(actual.isIncomingShare).isEqualTo(photo.isIncomingShare)
        assertThat(actual.isNodeKeyDecrypted).isEqualTo(photo.isNodeKeyDecrypted)
        assertThat(actual.creationTime).isEqualTo(
            photo.creationTime.atZone(ZoneId.systemDefault()).toEpochSecond()
        )
        assertThat(actual.serializedData).isEqualTo(photo.serializedData)
        assertThat(actual.isAvailableOffline).isEqualTo(photo.isAvailableOffline)
        assertThat(actual.versionCount).isEqualTo(photo.versionCount)
        assertThat(actual.description).isEqualTo(photo.description)
        assertThat(actual.tags).isEqualTo(photo.tags)
    }
}
