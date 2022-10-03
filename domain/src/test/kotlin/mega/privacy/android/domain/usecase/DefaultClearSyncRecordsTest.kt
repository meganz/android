package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultClearSyncRecordsTest {
    private lateinit var underTest: ClearSyncRecords

    private val cameraUploadRepository = mock<CameraUploadRepository>()


    @Before
    fun setUp() {
        underTest = DefaultClearSyncRecords(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that sync records are deleted when sync records should be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(true)
            underTest()
            verify(cameraUploadRepository,
                times(1)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(1)).saveShouldClearCamSyncRecords(false)
        }

    @Test
    fun `test that sync records are not deleted when sync records should not be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(false)
            underTest()
            verify(cameraUploadRepository,
                times(0)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(0)).saveShouldClearCamSyncRecords(false)
        }
}