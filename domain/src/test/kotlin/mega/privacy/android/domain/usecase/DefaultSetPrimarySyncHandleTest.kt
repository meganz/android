package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSyncHandleUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class DefaultSetPrimarySyncHandleTest {
    private lateinit var underTest: SetPrimarySyncHandle

    private val setupCameraUploadsSyncHandleUseCase = mock<SetupCameraUploadsSyncHandleUseCase>()
    private val updateFolderIconBroadcast = mock<UpdateFolderIconBroadcast>()
    private val updateFolderDestinationBroadcast = mock<UpdateFolderDestinationBroadcast>()

    @Before
    fun setUp() {
        underTest = DefaultSetPrimarySyncHandle(
            setupCameraUploadsSyncHandleUseCase = setupCameraUploadsSyncHandleUseCase,
            updateFolderIconBroadcast = updateFolderIconBroadcast,
            updateFolderDestinationBroadcast = updateFolderDestinationBroadcast
        )
    }

    @Test
    fun `test that primary sync handle is set and folder icons and destinations are updated if primary sync handle is updated`() =
        runTest {
            val result = 69L
            underTest(result)
            verify(setupCameraUploadsSyncHandleUseCase).invoke(result)
            verify(updateFolderIconBroadcast).invoke(result, false)
            verify(updateFolderDestinationBroadcast).invoke(result, false)
        }
}
