package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesUriPathHaveSufficientSpaceForNodesUseCaseTest {
    private lateinit var underTest: DoesUriPathHaveSufficientSpaceForNodesUseCase

    private val totalFileSizeOfNodesUseCase = mock<TotalFileSizeOfNodesUseCase>()
    private val doesPathHaveSufficientSpaceUseCase = mock<DoesPathHaveSufficientSpaceUseCase>()
    private val nodes = mock<List<TypedNode>>()
    val fileSystemRepository = mock<FileSystemRepository>()


    @BeforeAll
    fun setup() {
        underTest = DoesUriPathHaveSufficientSpaceForNodesUseCase(
            totalFileSizeOfNodesUseCase,
            doesPathHaveSufficientSpaceUseCase,
            fileSystemRepository
        )
    }

    @BeforeEach
    fun resetMocks() = reset(totalFileSizeOfNodesUseCase, doesPathHaveSufficientSpaceUseCase, nodes)

    @Test
    fun `test that totalFileSizeOfNodesUseCase is called with the list of nodes`() = runTest {
        stubTotalFileSize()
        stubUriPath(PATH)
        underTest(uriPath, nodes)
        verify(totalFileSizeOfNodesUseCase).invoke(nodes)
    }

    @Test
    fun `test that doesPathHaveSufficientSpaceUseCase is called with the proper path when is a path`() =
        runTest {
            stubTotalFileSize()
            stubUriPath(PATH)
            underTest(uriPath, nodes)
            verify(doesPathHaveSufficientSpaceUseCase).invoke(eq(PATH), any())
        }

    @Test
    fun `test that doesPathHaveSufficientSpaceUseCase is called with path from repository when is a file uri`() =
        runTest {
            stubTotalFileSize()
            stubUriPath(uriFile.value, isFileUri = true)
            whenever(fileSystemRepository.getFileFromFileUri(uriFile.value)) doReturn File(PATH)
            underTest(uriFile, nodes)
            verify(doesPathHaveSufficientSpaceUseCase).invoke(eq(PATH), any())
        }

    @Test
    fun `test that doesPathHaveSufficientSpaceUseCase is called with path from repository when is an external content uri`() =
        runTest {
            stubTotalFileSize()
            stubUriPath(uriExternalContent.value, isExternalContentUri = true)
            whenever(fileSystemRepository.getExternalPathByContentUri(uriExternalContent.value)) doReturn PATH
            underTest(uriExternalContent, nodes)
            verify(doesPathHaveSufficientSpaceUseCase).invoke(eq(PATH), any())
        }

    @Test
    fun `test that doesPathHaveSufficientSpaceUseCase is called with the resulting nodes size`() =
        runTest {
            stubTotalFileSize()
            stubUriPath(PATH)
            underTest(uriPath, nodes)
            verify(doesPathHaveSufficientSpaceUseCase).invoke(any(), eq(TOTAL_SIZE))
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that doesPathHaveSufficientSpaceUseCase result is returned`(expectedHaveEnoughSpace: Boolean) =
        runTest {
            stubTotalFileSize()
            whenever(doesPathHaveSufficientSpaceUseCase.invoke(PATH, TOTAL_SIZE)).thenReturn(
                expectedHaveEnoughSpace
            )
            val actual = underTest(uriPath, nodes)
            Truth.assertThat(actual).isEqualTo(expectedHaveEnoughSpace)
        }

    private suspend fun stubTotalFileSize() {
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(TOTAL_SIZE)
    }

    private suspend fun stubUriPath(
        uriPathString: String,
        isExternalContentUri: Boolean = false,
        isFileUri: Boolean = false,
    ) {
        whenever(fileSystemRepository.isExternalStorageContentUri(uriPathString)) doReturn isExternalContentUri
        whenever(fileSystemRepository.isFileUri(uriPathString)) doReturn isFileUri
    }

    companion object {
        private const val PATH = "/root"
        private val uriPath = UriPath(PATH)
        private val uriFile = UriPath("file://$PATH")
        private val uriExternalContent = UriPath("content://external.file")
        private const val TOTAL_SIZE = 1024L
    }
}