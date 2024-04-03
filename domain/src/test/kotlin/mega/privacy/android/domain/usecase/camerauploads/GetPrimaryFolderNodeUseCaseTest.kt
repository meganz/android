package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.CameraUploadRepository
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
 * Test class for [GetPrimaryFolderNodeUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetPrimaryFolderNodeUseCaseTest {

    private lateinit var underTest: GetPrimaryFolderNodeUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val setupCameraUploadsSyncHandleUseCase = mock<SetupCameraUploadsSyncHandleUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetPrimaryFolderNodeUseCase(
            cameraUploadRepository = cameraUploadRepository,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            setupCameraUploadsSyncHandleUseCase = setupCameraUploadsSyncHandleUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            getNodeByIdUseCase,
            getPrimarySyncHandleUseCase,
            setupCameraUploadsSyncHandleUseCase,
        )
    }

    @Test
    fun `test that providing a node id does not require a call to retrieve the primary folder handle`() =
        runTest {
            underTest(NodeId(123456L))

            verifyNoInteractions(getPrimarySyncHandleUseCase)
        }

    @Test
    fun `test that null is returned when no node id is provided and the primary folder handle retrieved is invalid`() =
        runTest {
            val invalidHandle = -1L
            whenever(getPrimarySyncHandleUseCase()).thenReturn(invalidHandle)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)

            assertThat(underTest()).isNull()
        }

    @Test
    fun `test that null is returned when the node id provided is invalid`() = runTest {
        val invalidHandle = -1L
        whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)

        assertThat(underTest(NodeId(invalidHandle))).isNull()
    }

    @Test
    fun `test that null is returned when the primary folder node retrieved from the provided valid node id is null`() =
        runTest {
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(null)

            assertThat(underTest(NodeId(123456L))).isNull()
        }

    @Test
    fun `test that null is returned when the primary folder node retrieved from a valid primary folder handle api call is null`() =
        runTest {
            whenever(getPrimarySyncHandleUseCase()).thenReturn(123456L)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(null)

            assertThat(underTest()).isNull()
        }

    @Test
    fun `test that the primary folder handle is always invalidated when the primary folder node retrieved is null`() =
        runTest {
            val invalidHandle = -1L
            whenever(getPrimarySyncHandleUseCase()).thenReturn(123456L)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(null)

            underTest()

            verify(setupCameraUploadsSyncHandleUseCase).invoke(invalidHandle)
        }

    @Test
    fun `test that the primary folder node is retrieved using a valid primary folder handle retrieved from the api`() =
        runTest {
            val expectedNode = mock<TypedFolderNode>()
            whenever(getPrimarySyncHandleUseCase()).thenReturn(123456L)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(expectedNode)

            assertThat(underTest()).isEqualTo(expectedNode)
        }

    @Test
    fun `test that the primary folder node is retrieved using the provided primary folder node id`() =
        runTest {
            val expectedNode = mock<TypedFolderNode>()
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(expectedNode)

            assertThat(underTest(NodeId(123456L))).isEqualTo(expectedNode)
        }
}