package mega.privacy.android.domain.usecase.transfers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentMetadata
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileDescriptorWrapperFromUriPathUseCaseTest {

    private lateinit var underTest: GetFileDescriptorWrapperFromUriPathUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFileDescriptorWrapperFromUriPathUseCase(
            fileSystemRepository
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            fileSystemRepository,
        )

    @Test
    fun `test that null is returned if document metadata from repository is null`() = runTest {
        val uriPath = UriPath("content://file")
        whenever(fileSystemRepository.getDocumentMetadata(uriPath)) doReturn null

        val actual = underTest(uriPath)

        assertThat(actual?.name).isNull()
    }

    @Test
    fun `test that name is correctly set from repository`() = runTest {
        val uriPath = UriPath("content://file")
        val expectedName = "name"
        whenever(fileSystemRepository.getDocumentMetadata(uriPath)) doReturn
                DocumentMetadata(expectedName)

        val actual = underTest(uriPath)

        assertThat(actual?.name).isEqualTo(expectedName)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isFolder is correctly set from repository`(
        isFolder: Boolean,
    ) = runTest {
        val uriPath = UriPath("content://file")
        whenever(fileSystemRepository.getDocumentMetadata(uriPath)) doReturn
                DocumentMetadata("name", isFolder)

        val actual = underTest(uriPath)

        assertThat(actual?.isFolder).isEqualTo(isFolder)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that file descriptor is fetched from the repository in a lazy way`(
        write: Boolean,
    ) = runTest {
        val uriPath = UriPath("content://file")
        val expected = 6354
        whenever(fileSystemRepository.getDocumentMetadata(uriPath)) doReturn
                DocumentMetadata("name")
        whenever(fileSystemRepository.getDetachedFileDescriptor(uriPath, write)) doReturn
                expected

        val actual = underTest(uriPath)

        verify(fileSystemRepository, times(0)).getDetachedFileDescriptor(uriPath, write)
        assertThat(actual?.getDetachedFileDescriptor(write)).isEqualTo(expected)

        assertThat(actual?.getDetachedFileDescriptor(write)).isEqualTo(expected)
        verify(fileSystemRepository, times(1)).getDetachedFileDescriptor(uriPath, write)
    }

    @Test
    fun `test that children uris are not fetched when it is a file`() = runTest {
        val uriPath = UriPath("content://file")
        whenever(fileSystemRepository.getDocumentMetadata(uriPath)) doReturn
                DocumentMetadata("name")

        val actual = underTest(uriPath)

        assertThat(actual?.getChildrenUris()).isEmpty()
        verify(fileSystemRepository, times(0)).getFolderChildUriPaths(uriPath)
    }

    @Test
    fun `test that children uris are fetched from the repository on demand when it is a folder`() =
        runTest {
            val uriPath = UriPath("content://folder")
            whenever(fileSystemRepository.getDocumentMetadata(uriPath)) doReturn
                    DocumentMetadata("name", true)
            val expected1 = listOf(UriPath("content://child1"))
            whenever(fileSystemRepository.getFolderChildUriPaths(uriPath)) doReturn
                    expected1

            val actual = underTest(uriPath)

            verify(fileSystemRepository, times(0)).getFolderChildUriPaths(uriPath)
            assertThat(actual?.getChildrenUris()).isEqualTo(expected1)

            val expected2 = listOf(UriPath("content://child2"))
            whenever(fileSystemRepository.getFolderChildUriPaths(uriPath)) doReturn
                    expected2
            val actual2 = underTest(uriPath)
            assertThat(actual2?.getChildrenUris()).isEqualTo(expected2)
        }
}