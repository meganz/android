package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.zipbrowser.ZipTreeNodeMapper
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.repository.ZipBrowserRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipBrowserRepositoryImplTest {
    @TempDir
    lateinit var temporaryFolder: File

    private lateinit var underTest: ZipBrowserRepository

    private val zipTreeNodeMapper = mock<ZipTreeNodeMapper>()

    @BeforeAll
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = ZipBrowserRepositoryImpl(
            zipTreeNodeMapper = zipTreeNodeMapper,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(zipTreeNodeMapper)
    }

    @Test
    fun `test that the ZipNodeTree returns empty`() = runTest {
        val mockEnumeration = mock<Enumeration<out ZipEntry>>()
        val testZipFile = mock<ZipFile> {
            on { entries() }.thenReturn(mockEnumeration)
        }
        whenever(mockEnumeration.hasMoreElements()).thenReturn(false)
        val actual = underTest.getZipNodeTree(testZipFile)
        assertThat(actual).isEmpty()
    }

    @Test
    fun `test that the ZipNodeTree is returned`() = runTest {
        val testEnumeration = mock<Enumeration<out ZipEntry>>()
        val testZipFile = mock<ZipFile> {
            on { entries() }.thenReturn(testEnumeration)
        }

        val testPath1 = "zipFolder/"
        val testPath2 = "zipFolder/zipSubFolder/"
        val testPath3 = "zipFolder/zipSubFolder/zipFile.zip"
        val testPath4 = "zipFolder/zipSubFolder/file.txt"


        val testEntry1 = initZipEntry(testPath1, true)
        val testEntry2 = initZipEntry(testPath2, true)
        val testEntry3 = initZipEntry(testPath3, false)
        val testEntry4 = initZipEntry(testPath4, false)

        whenever(testEnumeration.hasMoreElements()).thenReturn(true, true, true, true, false)
        whenever(testEnumeration.nextElement()).thenReturn(
            testEntry1,
            testEntry2,
            testEntry3,
            testEntry4,
        )

        val actual = underTest.getZipNodeTree(testZipFile)
        assertThat(actual).isNotEmpty()
        assertThat(actual.keys.size).isEqualTo(4)
    }

    private fun initZipEntry(
        expectedName: String,
        expectedIsDirectory: Boolean,
    ) = mock<ZipEntry> {
        on { name }.thenReturn(expectedName)
        on { size }.thenReturn(100L)
        on { isDirectory }.thenReturn(expectedIsDirectory)
    }

    @Test
    fun `test that the ZipNodeTree is returned correctly when zip entry is the file`() =
        runTest {
            val testEnumeration = mock<Enumeration<out ZipEntry>>()
            val testZipFile = mock<ZipFile> {
                on { entries() }.thenReturn(testEnumeration)
            }

            val testPath = "file.txt"
            val testEntry = initZipEntry(testPath, false)
            val testZipTreeNode = initZipTreeNode(testPath, testPath, ZipEntryType.File)

            whenever(testEnumeration.hasMoreElements()).thenReturn(true, false)
            whenever(testEnumeration.nextElement()).thenReturn(testEntry)
            whenever(
                zipTreeNodeMapper(
                    testEntry,
                    testPath,
                    testPath,
                    null,
                    ZipEntryType.File
                )
            ).thenReturn(
                testZipTreeNode
            )

            val actual = underTest.getZipNodeTree(testZipFile)
            assertThat(actual).isNotEmpty()
            assertThat(actual.keys.size).isEqualTo(1)
            assertThat(actual[testPath]).isEqualTo(testZipTreeNode)
        }

    private fun initZipTreeNode(
        expectedName: String,
        expectedPath: String,
        expectedZipEntryType: ZipEntryType,
    ) = mock<ZipTreeNode> {
        on { name }.thenReturn(expectedName)
        on { size }.thenReturn(100L)
        on { path }.thenReturn(expectedPath)
        on { parentPath }.thenReturn(null)
        on { zipEntryType }.thenReturn(expectedZipEntryType)
    }

    @Test
    fun `test that the ZipNodeTree is returned correctly when zip entry is the folder`() =
        runTest {
            val testEnumeration = mock<Enumeration<out ZipEntry>>()
            val testZipFile = mock<ZipFile> {
                on { entries() }.thenReturn(testEnumeration)
            }

            val testPath = "zipFolder/"
            val testName = testPath.removeSuffix("/")
            val testEntry = initZipEntry(testPath, true)
            val testZipTreeNode = initZipTreeNode(testPath, testPath, ZipEntryType.Folder)

            whenever(testEnumeration.hasMoreElements()).thenReturn(true, false)
            whenever(testEnumeration.nextElement()).thenReturn(testEntry)
            whenever(
                zipTreeNodeMapper(
                    testEntry,
                    testName,
                    testName,
                    null,
                    ZipEntryType.Folder
                )
            ).thenReturn(
                testZipTreeNode
            )

            val actual = underTest.getZipNodeTree(testZipFile)
            assertThat(actual).isNotEmpty()
            assertThat(actual.keys.size).isEqualTo(1)
            assertThat(actual[testName]).isEqualTo(testZipTreeNode)
        }

    @Test
    fun `test that the unzipFile function returns false`() =
        runTest {
            val testEnumeration = mock<Enumeration<out ZipEntry>>()
            val testZipFile = mock<ZipFile> {
                on { entries() }.thenReturn(testEnumeration)
            }

            val testPath = "zipFolder/"
            val testEntry = initZipEntry(testPath, true)

            whenever(testEnumeration.hasMoreElements()).thenReturn(true, false)
            whenever(testEnumeration.nextElement()).thenReturn(testEntry)

            val actual = underTest.unzipFile(testZipFile, testPath)
            assertThat(actual).isFalse()
        }

    @Test
    fun `test that the unzipFile function returns true`() =
        runTest {
            val testEnumeration = mock<Enumeration<out ZipEntry>>()
            val testName = "zipFile/"
            val testPath = File(temporaryFolder, testName)
            val testZipFile = mock<ZipFile> {
                on { entries() }.thenReturn(testEnumeration)
            }

            val testEntry = initZipEntry(testName, true)

            whenever(testEnumeration.hasMoreElements()).thenReturn(true, false)
            whenever(testEnumeration.nextElement()).thenReturn(testEntry)

            val actual = underTest.unzipFile(testZipFile, testPath.canonicalPath)
            assertThat(actual).isTrue()
        }

    @Test
    fun `test that the unzipFile function returns false when an Exception is raised`() =
        runTest {
            val testEnumeration = mock<Enumeration<out ZipEntry>>()
            val testName = "zipFile/"
            val testPath = File(temporaryFolder, testName).absolutePath
            val testZipFile = mock<ZipFile> {
                on { entries() }.thenReturn(testEnumeration)
            }

            val testEntry = initZipEntry(testName, false)

            whenever(testEnumeration.hasMoreElements()).thenReturn(true, false)
            whenever(testEnumeration.nextElement()).thenReturn(testEntry)
            whenever(testZipFile.getInputStream(anyOrNull())).thenThrow(NullPointerException())

            val actual = underTest.unzipFile(testZipFile, testPath)
            assertThat(actual).isFalse()
        }
}