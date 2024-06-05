package mega.privacy.android.data.mapper.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import java.util.zip.ZipEntry

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipTreeNodeMapperTest {
    private lateinit var underTest: ZipTreeNodeMapper

    private val testName = "zip entry"
    private val testZipFileName = "zip entry.zip"
    private val testPath = "zip path"
    private val testParentPath = "parent path"
    private val testSize = 100L

    @BeforeAll
    fun setUp() {
        underTest = ZipTreeNodeMapper()
    }

    @Test
    fun `test that ZipTreeNode can be mapped correctly when ZipEntryType is Folder`() =
        runTest {
            val testZipEntry = mock<ZipEntry> {
                on { size }.thenReturn(testSize)
                on { isDirectory }.thenReturn(true)
            }
            val testEntryType = ZipEntryType.Folder

            val zipTreeNode = underTest(
                zipEntry = testZipEntry,
                name = testName,
                path = testPath,
                parentPath = testParentPath,
                zipEntryType = testEntryType
            )
            assertZipTreeNodeMapperMapper(
                zipTreeNode = zipTreeNode,
                expectedZipEntryType = testEntryType
            )
        }

    @Test
    fun `test that ZipTreeNode can be mapped correctly when ZipEntryType is Zip`() =
        runTest {
            val testZipEntry = mock<ZipEntry> {
                on { size }.thenReturn(testSize)
                on { isDirectory }.thenReturn(false)
            }
            val testEntryType = ZipEntryType.Zip

            val zipTreeNode = underTest(
                zipEntry = testZipEntry,
                name = testZipFileName,
                path = testPath,
                parentPath = testParentPath,
                zipEntryType = testEntryType,
            )
            assertZipTreeNodeMapperMapper(
                zipTreeNode = zipTreeNode,
                expectedName = testZipFileName,
                expectedZipEntryType = testEntryType
            )
        }

    @Test
    fun `test that ZipTreeNode can be mapped correctly when ZipEntryType is File`() =
        runTest {
            val testZipEntry = mock<ZipEntry> {
                on { size }.thenReturn(testSize)
                on { isDirectory }.thenReturn(false)
            }
            val testEntryType = ZipEntryType.File

            val zipTreeNode = underTest(
                zipEntry = testZipEntry,
                name = testName,
                path = testPath,
                parentPath = testParentPath,
                zipEntryType = testEntryType
            )
            assertZipTreeNodeMapperMapper(
                zipTreeNode = zipTreeNode,
                expectedZipEntryType = testEntryType
            )
        }

    private fun assertZipTreeNodeMapperMapper(
        zipTreeNode: ZipTreeNode,
        expectedName: String = testName,
        expectedZipEntryType: ZipEntryType,
    ) {
        zipTreeNode.let {
            Assertions.assertAll(
                "Grouped Assertions of ${zipTreeNode::class.simpleName}",
                { assertThat(it.name).isEqualTo(expectedName) },
                { assertThat(it.path).isEqualTo(testPath) },
                { assertThat(it.parentPath).isEqualTo(testParentPath) },
                { assertThat(it.zipEntryType).isEqualTo(expectedZipEntryType) },
                { assertThat(it.size).isEqualTo(testSize) },
                { assertThat(it.children).isEmpty() }
            )
        }
    }
}