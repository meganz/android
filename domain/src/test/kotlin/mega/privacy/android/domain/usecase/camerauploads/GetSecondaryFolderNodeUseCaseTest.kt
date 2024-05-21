package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [GetSecondaryFolderNodeUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSecondaryFolderNodeUseCaseTest {

    private lateinit var underTest: GetSecondaryFolderNodeUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val setupMediaUploadsSyncHandleUseCase = mock<SetupMediaUploadsSyncHandleUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetSecondaryFolderNodeUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            setupMediaUploadsSyncHandleUseCase = setupMediaUploadsSyncHandleUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadsRepository,
            getNodeByIdUseCase,
            getSecondarySyncHandleUseCase,
            setupMediaUploadsSyncHandleUseCase,
        )
    }

    @Test
    fun `test that providing a node id does not require a call to retrieve the secondary folder handle`() =
        runTest {
            underTest(NodeId(123456L))

            verifyNoInteractions(getSecondarySyncHandleUseCase)
        }

    @Test
    fun `test that null is returned when no node id is provided and the secondary folder handle retrieved is invalid`() =
        runTest {
            val invalidHandle = -1L
            whenever(getSecondarySyncHandleUseCase()).thenReturn(invalidHandle)
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(invalidHandle)

            assertThat(underTest()).isNull()
        }

    @Test
    fun `test that null is returned when the node id provided is invalid`() = runTest {
        val invalidHandle = -1L
        whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(invalidHandle)

        assertThat(underTest(NodeId(invalidHandle))).isNull()
    }

    @Test
    fun `test that null is returned when the secondary folder node retrieved from the provided valid node id is null`() =
        runTest {
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(null)

            assertThat(underTest(NodeId(123456L))).isNull()
        }

    @Test
    fun `test that null is returned when the secondary folder node retrieved from a valid secondary folder handle api call is null`() =
        runTest {
            whenever(getSecondarySyncHandleUseCase()).thenReturn(123456L)
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(null)

            assertThat(underTest()).isNull()
        }

    @Test
    fun `test that the secondary folder handle is always invalidated when the secondary folder node retrieved is null`() =
        runTest {
            val invalidHandle = -1L
            whenever(getSecondarySyncHandleUseCase()).thenReturn(123456L)
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(null)

            underTest()

            verify(setupMediaUploadsSyncHandleUseCase).invoke(invalidHandle)
        }

    @Test
    fun `test that the secondary folder node is retrieved using a valid secondary folder handle retrieved from the api`() =
        runTest {
            val expectedNode = mock<TypedFolderNode>()
            whenever(getSecondarySyncHandleUseCase()).thenReturn(123456L)
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(expectedNode)

            assertThat(underTest()).isEqualTo(expectedNode)
        }

    @Test
    fun `test that the secondary folder node is retrieved using the provided secondary folder node id`() =
        runTest {
            val expectedNode = mock<TypedFolderNode>()
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(expectedNode)

            assertThat(underTest(NodeId(123456L))).isEqualTo(expectedNode)
        }
}