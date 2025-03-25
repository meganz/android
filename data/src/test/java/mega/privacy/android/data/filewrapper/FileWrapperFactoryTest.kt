package mega.privacy.android.data.filewrapper

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.document.DocumentMetadata
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.use

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileWrapperFactoryTest {

    private lateinit var underTest: FileWrapperFactory

    private val fileGateway = mock<FileGateway>()

    @BeforeAll
    fun setup() {
        underTest = FileWrapperFactory(
            fileGateway
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            fileGateway,
        )

    @Test
    fun `test that null is returned if document metadata from gateway is null`() =
        Mockito.mockStatic(Uri::class.java).useNoResult {
            val (uriPath, _) = commonStub(documentMetadata = null)

            val actual = underTest(uriPath)

            assertThat(actual?.name).isNull()
        }


    @Test
    fun `test that name is correctly set from gateway`() =
        Mockito.mockStatic(Uri::class.java).useNoResult {
            val expectedName = "name"
            val (uriPath, _) = commonStub(documentMetadata = DocumentMetadata(expectedName))

            val actual = underTest(uriPath)

            assertThat(actual?.name).isEqualTo(expectedName)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isFolder is correctly set from gateway`(
        isFolder: Boolean,
    ) = Mockito.mockStatic(Uri::class.java).useNoResult {
        val (uriPath, _) = commonStub(documentMetadata = DocumentMetadata("name", isFolder))

        val actual = underTest(uriPath)

        assertThat(actual?.isFolder).isEqualTo(isFolder)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that file descriptor is fetched from the gateway in a lazy way`(
        write: Boolean,
    ) = Mockito.mockStatic(Uri::class.java).useNoResult {
        val (uriPath, _) = commonStub()
        val expected = 59845
        val fileDescriptor = mock<ParcelFileDescriptor> {
            on { detachFd() } doReturn expected
        }
        whenever(fileGateway.getFileDescriptorSync(uriPath, write)) doReturn
                fileDescriptor

        val actual = underTest(uriPath)

        verify(fileGateway, times(0)).getFileDescriptorSync(uriPath, write)
        assertThat(actual?.getFileDescriptor(write)).isEqualTo(expected)
    }

    @Test
    fun `test that children uris are not fetched when it is a file`() =
        Mockito.mockStatic(Uri::class.java).useNoResult {
            val (uriPath, uri) = commonStub()

            val actual = underTest(uriPath)

            assertThat(actual?.getChildrenUris()).isEmpty()
            verify(fileGateway, times(0)).getFolderChildUrisSync(uri)
        }

    @Test
    fun `test that children uris are fetched from the gateway on demand when it is a folder`() =
        Mockito.mockStatic(Uri::class.java).useNoResult {
            val (uriPath, uri) = commonStub(
                "content://folder",
                documentMetadata = DocumentMetadata("name", true)
            )

            val (uriPathChild1, uriChild1) = commonStub("content://child1")
            whenever(fileGateway.getFolderChildUrisSync(uri)) doReturn
                    listOf(uriChild1)

            val actual = underTest(uriPath)

            verify(fileGateway, times(0)).getFolderChildUrisSync(uri)
            assertThat(actual?.getChildrenUris()).containsExactly(uriPathChild1.value)

            val (uriPathChild2, uriChild2) = commonStub("content://child2")
            whenever(fileGateway.getFolderChildUrisSync(uri)) doReturn
                    listOf(uriChild2)
            val actual2 = underTest(uriPath)
            assertThat(actual2?.getChildrenUris()).containsExactly(uriPathChild2.value)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that child existence are checked from the gateway`(
        expected: Boolean,
    ) = Mockito.mockStatic(Uri::class.java).useNoResult {
        val (uriPath, _) = commonStub(
            "content://folder",
            documentMetadata = DocumentMetadata("parent", true)
        )
        val childName = "foo"
        whenever(fileGateway.childFileExistsSync(uriPath, childName)) doReturn expected
        val actual = underTest(uriPath)?.childFileExists(childName)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that child is correctly created from gateway child`(
        asFolder: Boolean,
    ) = Mockito.mockStatic(Uri::class.java).useNoResult {
        val childName = "child"
        val (uriPath, _) = commonStub("content://folder", DocumentMetadata("parent", true))
        val (uriPathChild, _) = commonStub(
            documentMetadata = DocumentMetadata(
                childName,
                asFolder
            )
        )

        whenever(
            fileGateway.createChildFileSync(
                uriPath,
                childName,
                asFolder
            )
        ) doReturn uriPathChild

        val actual = underTest(uriPath)?.createChildFile(childName, asFolder)

        assertThat(actual?.name).isEqualTo(childName)
        assertThat(actual?.uri).isEqualTo(uriPathChild.value)
        assertThat(actual?.isFolder).isEqualTo(asFolder)
    }

    @Test
    fun `test that parent is returned from the gateway`() =
        Mockito.mockStatic(Uri::class.java).useNoResult {
            val (uriPath, _) =
                commonStub(documentMetadata = DocumentMetadata("parent", true))
            val (uriPathParent, _) = commonStub("content://folder")

            whenever(fileGateway.getParentSync(uriPath)) doReturn uriPathParent
            val actual = underTest(uriPath)?.getParentFile()
            assertThat(actual?.uri).isEqualTo(uriPathParent.value)
        }

    @Test
    fun `test that path is returned from the gateway`() =
        Mockito.mockStatic(Uri::class.java).useNoResult {
            val (uriPath, _) = commonStub()
            val expected = "/path"
            whenever(fileGateway.getExternalPathByContentUriSync(uriPath.value)) doReturn expected
            val actual = underTest(uriPath)?.getPath()
            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that delete file invokes the correct gateway method`(
        result: Boolean,
    ) = Mockito.mockStatic(Uri::class.java).useNoResult {
        val (uriPath, _) = commonStub()
        whenever(fileGateway.deleteIfItIsAFileSync(uriPath)) doReturn result
        val actual = underTest(uriPath)?.deleteFile()
        assertThat(actual).isEqualTo(result)
        verify(fileGateway).deleteIfItIsAFileSync(uriPath)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that delete folder invokes the correct gateway method`(
        result: Boolean,
    ) = Mockito.mockStatic(Uri::class.java).useNoResult {
        val (uriPath, _) = commonStub()
        whenever(fileGateway.deleteIfItIsAnEmptyFolder(uriPath)) doReturn result
        val actual = underTest(uriPath)?.deleteFolderIfEmpty()
        assertThat(actual).isEqualTo(result)
        verify(fileGateway).deleteIfItIsAnEmptyFolder(uriPath)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that set modification time invokes the correct gateway method converting seconds to milliseconds`(
        result: Boolean,
    ) = Mockito.mockStatic(Uri::class.java).useNoResult {
        val (uriPath, _) = commonStub()
        val mTime = 4985739254L
        val mTimeMillis = mTime * 1_000
        whenever(fileGateway.setLastModifiedSync(uriPath, mTimeMillis)) doReturn result
        val actual = underTest(uriPath)?.setModificationTime(mTime)
        assertThat(actual).isEqualTo(result)
        verify(fileGateway).setLastModifiedSync(uriPath, mTimeMillis)
    }

    @Test
    fun `test that renamed file is returned from the gateway`() =
        Mockito.mockStatic(Uri::class.java).useNoResult {
            val (uriPath, _) = commonStub()
            val (uriPathRenamed, _) = commonStub("content://renamed")
            val newName = "renamed"

            whenever(fileGateway.renameFileSync(uriPath, newName)) doReturn uriPathRenamed
            val actual = underTest(uriPath)?.rename(newName)
            assertThat(actual?.uri).isEqualTo(uriPathRenamed.value)
        }

    private fun commonStub(
        uriString: String = URI_STRING,
        documentMetadata: DocumentMetadata? = DocumentMetadata("name", false),
    ): Pair<UriPath, Uri> {
        val uriPath = UriPath(uriString)
        val uri = mock<Uri> {
            on { scheme } doReturn "content"
            on { toString() } doReturn uriString
        }
        whenever(Uri.parse(uriString)).thenReturn(uri)
        whenever(fileGateway.getDocumentMetadataSync(uri)) doReturn documentMetadata
        return uriPath to uri
    }

    inline fun <T : java.lang.AutoCloseable?, R> T.useNoResult(block: (T) -> R) {
        this.use(block)
    }

    companion object {
        const val URI_STRING = "content://file"
    }
}