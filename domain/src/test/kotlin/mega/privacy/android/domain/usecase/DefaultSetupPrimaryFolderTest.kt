package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultSetupPrimaryFolderTest {
    private lateinit var underTest: SetupPrimaryFolder
    private val invalidHandle = -1L

    private val cameraUploadRepository = mock<CameraUploadRepository> {
        onBlocking {
            getInvalidHandle()
        }.thenReturn(invalidHandle)
    }
    private val resetPrimaryTimeline = mock<ResetPrimaryTimeline>()
    private val setPrimarySyncHandle = mock<SetPrimarySyncHandle>()

    @Before
    fun setUp() {
        underTest = DefaultSetupPrimaryFolder(
            cameraUploadRepository = cameraUploadRepository,
            resetPrimaryTimeline = resetPrimaryTimeline,
            setPrimarySyncHandle = setPrimarySyncHandle
        )
    }

    @Test
    fun `test that if setup primary folder returns a success that primary attributes get updated`() =
        runTest {
            val result = 69L
            whenever(cameraUploadRepository.setupPrimaryFolder(any())).thenReturn(69L)
            underTest(any())
            verify(resetPrimaryTimeline).invoke()
            verify(setPrimarySyncHandle).invoke(result)
        }

    @Test
    fun `test that if setup primary folder returns an invalid handle that primary attributes do not update`() =
        runTest {
            whenever(cameraUploadRepository.setupPrimaryFolder(any())).thenReturn(invalidHandle)
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
