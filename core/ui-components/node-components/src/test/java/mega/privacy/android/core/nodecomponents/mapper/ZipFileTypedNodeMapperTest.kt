package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.ZipFileTypedNode
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipFileTypedNodeMapperTest {

    private lateinit var underTest: ZipFileTypedNodeMapper

    @BeforeEach
    fun setUp() {
        underTest = ZipFileTypedNodeMapper()
    }

    @Test
    fun `test that invoke returns ZipFileTypedNode`() {
        val file = File("/path/to/video.mp4")

        val result = underTest(file)

        assertThat(result).isInstanceOf(ZipFileTypedNode::class.java)
    }

    @Test
    fun `test that invoke maps name from file name`() {
        val file = File("/path/to/video.mp4")

        val result = underTest(file)

        assertThat(result.name).isEqualTo("video.mp4")
    }

    @Test
    fun `test that invoke maps type extension from file extension`() {
        val file = File("/path/to/video.mp4")

        val result = underTest(file)

        assertThat(result.type).isInstanceOf(UnMappedFileTypeInfo::class.java)
        assertThat(result.type.extension).isEqualTo("mp4")
    }

    @Test
    fun `test that invoke maps size from file length`() {
        val file = File("/path/to/video.mp4")

        val result = underTest(file)

        assertThat(result.size).isEqualTo(file.length())
    }

    @Test
    fun `test that invoke maps id from file hashCode`() {
        val file = File("/path/to/video.mp4")

        val result = underTest(file)

        assertThat(result.id.longValue).isEqualTo(file.name.hashCode().toLong())
    }

    @Test
    fun `test that invoke sets isTakenDown to false`() {
        val result = underTest(File("/path/to/video.mp4"))

        assertThat(result.isTakenDown).isFalse()
    }

    @Test
    fun `test that invoke sets isAvailableOffline to false`() {
        val result = underTest(File("/path/to/video.mp4"))

        assertThat(result.isAvailableOffline).isFalse()
    }

    @Test
    fun `test that invoke sets isNodeKeyDecrypted to true`() {
        val result = underTest(File("/path/to/video.mp4"))

        assertThat(result.isNodeKeyDecrypted).isTrue()
    }
}
