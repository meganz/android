package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.OfflineTypedFileNode
import mega.privacy.android.core.nodecomponents.model.OfflineTypedFolderNode
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineTypedNodeMapperTest {

    private lateinit var underTest: OfflineTypedNodeMapper

    @BeforeEach
    fun setUp() {
        underTest = OfflineTypedNodeMapper()
    }

    @Test
    fun `test that invoke returns OfflineTypedFileNode when offline info is a file`() {
        val offlineInfo = createOfflineFileInformation(isFolder = false)

        val result = underTest(offlineInfo)

        assertThat(result).isInstanceOf(OfflineTypedFileNode::class.java)
    }

    @Test
    fun `test that invoke returns OfflineTypedFolderNode when offline info is a folder`() {
        val offlineInfo = createOfflineFileInformation(isFolder = true)

        val result = underTest(offlineInfo)

        assertThat(result).isInstanceOf(OfflineTypedFolderNode::class.java)
    }

    @ParameterizedTest(name = "property {0} is correctly mapped")
    @MethodSource("providePropertyMappings")
    fun `test that invoke maps common node properties correctly`(
        propertyName: String,
        expectedValue: Any,
        offlineInfo: OfflineFileInformation,
    ) {
        val result = underTest(offlineInfo)

        val actualValue = when (propertyName) {
            "name" -> result.name
            "id" -> result.id.longValue
            "isAvailableOffline" -> result.isAvailableOffline
            "isTakenDown" -> result.isTakenDown
            "isNodeKeyDecrypted" -> result.isNodeKeyDecrypted
            else -> throw IllegalArgumentException("Unknown property: $propertyName")
        }

        assertThat(actualValue).isEqualTo(expectedValue)
    }

    @Test
    fun `test that invoke maps file node properties correctly`() {
        val offlineInfo = createOfflineFileInformation(
            name = "test.pdf",
            handle = "12345",
            totalSize = 1024L,
            fileTypeInfo = UnMappedFileTypeInfo("pdf"),
            thumbnail = "/path/to/thumbnail.jpg"
        )

        val result = underTest(offlineInfo) as OfflineTypedFileNode

        assertThat(result.name).isEqualTo("test.pdf")
        assertThat(result.id.longValue).isEqualTo(12345L)
        assertThat(result.size).isEqualTo(1024L)
        assertThat(result.thumbnailPath).isEqualTo("/path/to/thumbnail.jpg")
        assertThat(result.hasThumbnail).isTrue()
    }

    @Test
    fun `test that invoke returns hasThumbnail false when thumbnail is null`() {
        val offlineInfo = createOfflineFileInformation(thumbnail = null)

        val result = underTest(offlineInfo) as OfflineTypedFileNode

        assertThat(result.thumbnailPath).isNull()
        assertThat(result.hasThumbnail).isFalse()
    }

    @Test
    fun `test that invoke defaults type to UnMappedFileTypeInfo when fileTypeInfo is null`() {
        val offlineInfo = createOfflineFileInformation(
            name = "unknown.xyz",
            fileTypeInfo = null
        )

        val result = underTest(offlineInfo) as OfflineTypedFileNode

        assertThat(result.type).isInstanceOf(UnMappedFileTypeInfo::class.java)
        assertThat(result.type.extension).isEqualTo("xyz")
    }

    private fun providePropertyMappings(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "name",
            "testFile.txt",
            createOfflineFileInformation(name = "testFile.txt")
        ),
        Arguments.of(
            "id",
            67890L,
            createOfflineFileInformation(handle = "67890")
        ),
        Arguments.of(
            "isAvailableOffline",
            true,
            createOfflineFileInformation()
        ),
        Arguments.of(
            "isTakenDown",
            false,
            createOfflineFileInformation()
        ),
        Arguments.of(
            "isNodeKeyDecrypted",
            true,
            createOfflineFileInformation()
        ),
    )

    private fun createOfflineFileInformation(
        name: String = "test.txt",
        handle: String = "123",
        totalSize: Long = 100L,
        isFolder: Boolean = false,
        fileTypeInfo: Any? = UnMappedFileTypeInfo("txt"),
        thumbnail: String? = null,
    ): OfflineFileInformation {
        val nodeInfo = OtherOfflineNodeInformation(
            id = 1,
            path = "/offline/$name",
            name = name,
            handle = handle,
            isFolder = isFolder,
            lastModifiedTime = System.currentTimeMillis(),
            parentId = 0
        )
        return OfflineFileInformation(
            nodeInfo = nodeInfo,
            totalSize = totalSize,
            fileTypeInfo = fileTypeInfo as? mega.privacy.android.domain.entity.FileTypeInfo,
            thumbnail = thumbnail
        )
    }
}
