package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
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

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupSecondaryFolderUseCaseTest {
    private lateinit var underTest: SetupSecondaryFolderUseCase
    private val invalidHandle = -1L

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val setSecondarySyncHandle = mock<SetSecondarySyncHandle>()

    @BeforeAll
    fun setUp() {
        underTest = SetupSecondaryFolderUseCase(
            cameraUploadRepository = cameraUploadRepository,
            setSecondarySyncHandle = setSecondarySyncHandle
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            setSecondarySyncHandle,
        )
    }

    @Test
    fun `test that if setup secondary folder returns a success that secondary attributes get updated`() =
        runTest {
            val result = 69L
            whenever(cameraUploadRepository.setupSecondaryFolder(any())).thenReturn(69L)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            underTest(any())
            verify(setSecondarySyncHandle).invoke(result)
        }

    @Test
    fun `test that if setup secondary folder returns an invalid handle that secondary attributes do not update`() =
        runTest {
            whenever(cameraUploadRepository.setupSecondaryFolder(any())).thenReturn(invalidHandle)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            underTest(any())
            verify(cameraUploadRepository).setupSecondaryFolder(any())
            verify(cameraUploadRepository).getInvalidHandle()
            verifyNoInteractions(setSecondarySyncHandle)
        }

    @Test
    fun `test that if setup secondary folder returns an error, then throws an error`() =
        runTest {
            whenever(cameraUploadRepository.setupSecondaryFolder(any())).thenAnswer { throw Exception() }
            Assert.assertThrows(Exception::class.java) {
                runBlocking { underTest(any()) }
            }
        }
}
