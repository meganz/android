package mega.privacy.android.data.filewrapper

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
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

    private fun getUriAndUriPath(uriString: String = URI_STRING): Pair<UriPath, Uri> {
        val uriPath = UriPath(uriString)
        val uri = mock<Uri> {
            on { scheme } doReturn "content"
            on { toString()} doReturn uriString
        }
        whenever(Uri.parse(uriString)).thenReturn(uri)
        return uriPath to uri
    }

    @Test
    fun `test that null is returned if document metadata from gateway is null`() = runTest {  Mockito.mockStatic(Uri::class.java).use {
            val (uriPath, uri) = getUriAndUriPath()

            whenever(fileGateway.getDocumentMetadataSync(uri)) doReturn null

            val actual = underTest(uriPath)

            assertThat(actual?.name).isNull()
        }
    }


    @Test
    fun `test that name is correctly set from gateway`() = runTest {
        Mockito.mockStatic(Uri::class.java).use {
            val (uriPath, uri) = getUriAndUriPath()
            val expectedName = "name"
            whenever(fileGateway.getDocumentMetadataSync(uri)) doReturn
                    DocumentMetadata(expectedName)

            val actual = underTest(uriPath)

            assertThat(actual?.name).isEqualTo(expectedName)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isFolder is correctly set from gateway`(
        isFolder: Boolean,
    ) = runTest {
        Mockito.mockStatic(Uri::class.java).use {
            val (uriPath, uri) = getUriAndUriPath()
            whenever(fileGateway.getDocumentMetadataSync(uri)) doReturn
                    DocumentMetadata("name", isFolder)

            val actual = underTest(uriPath)

            assertThat(actual?.isFolder).isEqualTo(isFolder)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that file descriptor is fetched from the gateway in a lazy way`(
        write: Boolean,
    ) = runTest {
        Mockito.mockStatic(Uri::class.java).use {
            val (uriPath, uri) = getUriAndUriPath()
            val expected = 59845
            val fileDescriptor = mock<ParcelFileDescriptor>(){
                on {detachFd()} doReturn expected
            }
            whenever(fileGateway.getDocumentMetadataSync(uri)) doReturn
                    DocumentMetadata("name")
            whenever(fileGateway.getFileDescriptorSync(uriPath, write)) doReturn
                    fileDescriptor

            val actual = underTest(uriPath)

            verify(fileGateway, times(0)).getFileDescriptorSync(uriPath, write)
            assertThat(actual?.getFileDescriptor(write)).isEqualTo(expected)
        }
    }

    @Test
    fun `test that children uris are not fetched when it is a file`() = runTest {
        Mockito.mockStatic(Uri::class.java).use {
            val (uriPath, uri) = getUriAndUriPath()
            whenever(fileGateway.getDocumentMetadataSync(uri)) doReturn
                    DocumentMetadata("name")

            val actual = underTest(uriPath)

            assertThat(actual?.getChildrenUris()).isEmpty()
            verify(fileGateway, times(0)).getFolderChildUrisSync(uri)
        }
    }

    @Test
    fun `test that children uris are fetched from the gateway on demand when it is a folder`() =
        runTest {
            Mockito.mockStatic(Uri::class.java).use {
                val (uriPath, uri) = getUriAndUriPath("content://folder")
                whenever(fileGateway.getDocumentMetadataSync(uri)) doReturn
                        DocumentMetadata("name", true)
                val (uriPathChild1, uriChild1) = getUriAndUriPath("content://child1")
                whenever(fileGateway.getFolderChildUrisSync(uri)) doReturn
                        listOf(uriChild1)

                val actual = underTest(uriPath)

                verify(fileGateway, times(0)).getFolderChildUrisSync(uri)
                assertThat(actual?.getChildrenUris()).containsExactly(uriPathChild1.value)

                val (uriPathChild2, uriChild2) = getUriAndUriPath("content://child2")
                whenever(fileGateway.getFolderChildUrisSync(uri)) doReturn
                        listOf(uriChild2)
                val actual2 = underTest(uriPath)
                assertThat(actual2?.getChildrenUris()).containsExactly(uriPathChild2.value)
            }
        }

    companion object {
        val URI_STRING = "content://file"
    }
}