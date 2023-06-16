package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.ResetPrimaryTimeline
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
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
class SetupPrimaryFolderUseCaseTest {
    private lateinit var underTest: SetupPrimaryFolderUseCase
    private val invalidHandle = -1L

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val resetPrimaryTimeline = mock<ResetPrimaryTimeline>()
    private val setPrimarySyncHandle = mock<SetPrimarySyncHandle>()

    @BeforeAll
    fun setUp() {
        underTest = SetupPrimaryFolderUseCase(
            cameraUploadRepository = cameraUploadRepository,
            resetPrimaryTimeline = resetPrimaryTimeline,
            setPrimarySyncHandle = setPrimarySyncHandle
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            resetPrimaryTimeline,
            setPrimarySyncHandle,
        )
    }

    @Test
    fun `test that if setup primary folder returns a success that primary attributes get updated`() =
        runTest {
            val result = 69L
            whenever(cameraUploadRepository.setupPrimaryFolder(any())).thenReturn(69L)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            underTest(any())
            verify(resetPrimaryTimeline).invoke()
            verify(setPrimarySyncHandle).invoke(result)
        }

    @Test
    fun `test that if setup primary folder returns an invalid handle that primary attributes do not update`() =
        runTest {
            whenever(cameraUploadRepository.setupPrimaryFolder(any())).thenReturn(invalidHandle)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            underTest(any())
            verify(cameraUploadRepository).setupPrimaryFolder(any())
            verify(cameraUploadRepository).getInvalidHandle()
            verifyNoInteractions(setPrimarySyncHandle)
        }

    @Test
    fun `test that if setup primary folder returns an error, then throws an error`() =
        runTest {
            whenever(cameraUploadRepository.setupPrimaryFolder(any())).thenAnswer { throw Exception() }
            Assert.assertThrows(Exception::class.java) {
                runBlocking { underTest(any()) }
            }
        }
}
