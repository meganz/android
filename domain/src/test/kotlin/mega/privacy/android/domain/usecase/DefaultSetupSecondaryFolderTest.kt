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
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultSetupSecondaryFolderTest {
    private lateinit var underTest: SetupSecondaryFolder
    private val invalidHandle = -1L

    private val cameraUploadRepository = mock<CameraUploadRepository> {
        onBlocking {
            getInvalidHandle()
        }.thenReturn(invalidHandle)
    }
    private val resetSecondaryTimeline = mock<ResetSecondaryTimeline>()
    private val updateFolderIconBroadcast = mock<UpdateFolderIconBroadcast>()
    private val updateFolderDestinationBroadcast = mock<UpdateFolderDestinationBroadcast>()

    @Before
    fun setUp() {
        underTest = DefaultSetupSecondaryFolder(
            cameraUploadRepository = cameraUploadRepository,
            resetSecondaryTimeline = resetSecondaryTimeline,
            updateFolderIconBroadcast = updateFolderIconBroadcast,
            updateFolderDestinationBroadcast = updateFolderDestinationBroadcast
        )
    }

    @Test
    fun `test that if setup secondary folder returns a success that secondary attributes get updated`() =
        runTest {
            val result = 69L
            whenever(cameraUploadRepository.setupSecondaryFolder(any())).thenReturn(69L)
            underTest(any())
            verify(resetSecondaryTimeline).invoke()
            verify(cameraUploadRepository).setSecondarySyncHandle(result)
            verify(updateFolderIconBroadcast).invoke(result, true)
            verify(updateFolderDestinationBroadcast).invoke(result, true)
        }

    @Test
    fun `test that if setup secondary folder returns an invalid handle that secondary attributes do not update`() =
        runTest {
            whenever(cameraUploadRepository.setupSecondaryFolder(any())).thenReturn(invalidHandle)
            underTest(any())
            verify(cameraUploadRepository).setupSecondaryFolder(any())
            verify(cameraUploadRepository).getInvalidHandle()
            verifyNoMoreInteractions(cameraUploadRepository)
        }

    @Test
    fun `test that if setup secondary folder returns an error`() =
        runTest {
            whenever(cameraUploadRepository.setupPrimaryFolder(any())).thenAnswer { throw Exception() }
            Assert.assertThrows(Exception::class.java) {
                runBlocking { underTest(any()) }
            }
        }
}
