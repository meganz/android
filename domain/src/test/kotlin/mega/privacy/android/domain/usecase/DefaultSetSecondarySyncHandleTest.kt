package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class DefaultSetSecondarySyncHandleTest {
    private lateinit var underTest: SetSecondarySyncHandle

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val updateFolderIconBroadcast = mock<UpdateFolderIconBroadcast>()
    private val updateFolderDestinationBroadcast = mock<UpdateFolderDestinationBroadcast>()

    @Before
    fun setUp() {
        underTest = DefaultSetSecondarySyncHandle(
            cameraUploadRepository = cameraUploadRepository,
            updateFolderIconBroadcast = updateFolderIconBroadcast,
            updateFolderDestinationBroadcast = updateFolderDestinationBroadcast
        )
    }

    @Test
    fun `test that secondary sync handle is set and folder icons and destinations are updated if secondary sync handle is updated`() =
        runTest {
            val result = 69L
            underTest(result)
            verify(cameraUploadRepository).setSecondarySyncHandle(result)
            verify(updateFolderIconBroadcast).invoke(result, true)
            verify(updateFolderDestinationBroadcast).invoke(result, true)
        }
}
