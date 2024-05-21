package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.Assert
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupPrimaryFolderUseCaseTest {
    private lateinit var underTest: SetupPrimaryFolderUseCase
    private val invalidHandle = -1L

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val setPrimaryNodeIdUseCase = mock<SetPrimaryNodeIdUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetupPrimaryFolderUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            setPrimaryNodeIdUseCase = setPrimaryNodeIdUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadsRepository,
            setPrimaryNodeIdUseCase,
        )
    }

    @Test
    fun `test that if setup primary folder returns a success that primary attributes get updated`() =
        runTest {
            val result = 69L
            whenever(cameraUploadsRepository.setupPrimaryFolder(any())).thenReturn(69L)
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(invalidHandle)
            underTest(any())
            verify(setPrimaryNodeIdUseCase).invoke(NodeId(result))
        }

    @Test
    fun `test that if setup primary folder returns an invalid handle that primary attributes do not update`() =
        runTest {
            whenever(cameraUploadsRepository.setupPrimaryFolder(any())).thenReturn(invalidHandle)
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(invalidHandle)
            underTest(any())
            verify(cameraUploadsRepository).setupPrimaryFolder(any())
            verify(cameraUploadsRepository).getInvalidHandle()
            verifyNoInteractions(setPrimaryNodeIdUseCase)
        }

    @Test
    fun `test that if setup primary folder returns an error, then throws an error`() =
        runTest {
            whenever(cameraUploadsRepository.setupPrimaryFolder(any())).thenAnswer { throw Exception() }
            Assert.assertThrows(Exception::class.java) {
                runBlocking { underTest(any()) }
            }
        }
}
