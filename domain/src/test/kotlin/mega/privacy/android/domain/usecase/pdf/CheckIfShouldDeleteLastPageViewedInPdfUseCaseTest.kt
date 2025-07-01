package mega.privacy.android.domain.usecase.pdf

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.files.PdfRepository
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckIfShouldDeleteLastPageViewedInPdfUseCaseTest {

    private lateinit var underTest: CheckIfShouldDeleteLastPageViewedInPdfUseCase

    private val pdfRepository = mock<PdfRepository>()
    private val nodeRepository = mock<NodeRepository>()
    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = CheckIfShouldDeleteLastPageViewedInPdfUseCase(
            pdfRepository = pdfRepository,
            nodeRepository = nodeRepository,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(pdfRepository, nodeRepository, getFileTypeInfoByNameUseCase)
    }

    @Test
    fun `test that use case invokes correctly whe the node is a cloud pdf and there is no offline node related`() =
        runTest {
            val nodeHandle = 12345L
            val fileName = "test.pdf"
            val filetypeInfo = mock<PdfFileTypeInfo>()

            whenever(getFileTypeInfoByNameUseCase(fileName)) doReturn filetypeInfo
            whenever(nodeRepository.getOfflineNodeInformation(nodeHandle)) doReturn null

            underTest(nodeHandle = nodeHandle, fileName = fileName, isOfflineRemoval = false)

            verify(pdfRepository).deleteLastPageViewedInPdf(nodeHandle)
            verifyNoMoreInteractions(pdfRepository)
            verify(nodeRepository).getOfflineNodeInformation(nodeHandle)
            verifyNoMoreInteractions(nodeRepository)
        }

    @Test
    fun `test that use case invokes correctly whe the node is an offline pdf and there is no cloud node related`() =
        runTest {
            val nodeHandle = 12345L
            val nodeId = NodeId(nodeHandle)
            val fileName = "test.pdf"
            val filetypeInfo = mock<PdfFileTypeInfo>()

            whenever(getFileTypeInfoByNameUseCase(fileName)) doReturn filetypeInfo
            whenever(nodeRepository.getNodeById(nodeId)) doReturn null

            underTest(nodeHandle = nodeHandle, fileName = fileName, isOfflineRemoval = true)

            verify(pdfRepository).deleteLastPageViewedInPdf(nodeHandle)
            verifyNoMoreInteractions(pdfRepository)
            verify(nodeRepository).getNodeById(nodeId)
            verifyNoMoreInteractions(nodeRepository)
        }

    @Test
    fun `test that use case invokes correctly whe the offline node is not a pdf`() = runTest {
        val nodeHandle = 12345L
        val fileName = "test.pdf"
        val filetypeInfo = mock<UrlFileTypeInfo>()

        whenever(getFileTypeInfoByNameUseCase(fileName)) doReturn filetypeInfo

        underTest(nodeHandle = nodeHandle, fileName = fileName, isOfflineRemoval = true)

        verifyNoInteractions(pdfRepository)
        verifyNoInteractions(nodeRepository)
    }

    @Test
    fun `test that use case invokes correctly whe the cloud node is not a pdf`() = runTest {
        val nodeHandle = 12345L
        val fileName = "test.pdf"
        val filetypeInfo = mock<UrlFileTypeInfo>()

        whenever(getFileTypeInfoByNameUseCase(fileName)) doReturn filetypeInfo

        underTest(nodeHandle = nodeHandle, fileName = fileName, isOfflineRemoval = false)

        verifyNoInteractions(pdfRepository)
        verifyNoInteractions(nodeRepository)
    }
}