package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDisableCameraUploadSettingsTest {
    private lateinit var underTest: DisableCameraUploadSettings

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val settingsRepository = mock<SettingsRepository>()


    @Before
    fun setUp() {
        underTest = DefaultDisableCameraUploadSettings(
            cameraUploadRepository = cameraUploadRepository,
            settingsRepository = settingsRepository,
        )
    }

    @Test
    fun `test that invoke with true camera upload settings are updated`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(true)
            underTest(true)
            verify(cameraUploadRepository, times(1)).shouldClearSyncRecords()
            verify(settingsRepository,
                times(1)).setEnableCameraUpload(false)
            verify(cameraUploadRepository, times(1)).setSecondaryEnabled(false)
        }

    @Test
    fun `test that invoke with false camera upload settings are updated`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(true)
            underTest(true)
            verify(settingsRepository,
                times(1)).setEnableCameraUpload(false)
            verify(cameraUploadRepository, times(1)).setSecondaryEnabled(false)
        }

    @Test
    fun `test that invoke with true completes the operation successfully when sync records should be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(true)
            underTest(true)
            verify(cameraUploadRepository,
                times(1)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(1)).saveShouldClearCamSyncRecords(false)
        }

    @Test
    fun `test that invoke with true completes the operation successfully when sync records should not be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(false)
            underTest(true)
            verify(cameraUploadRepository,
                times(0)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(0)).saveShouldClearCamSyncRecords(false)
        }

    @Test
    fun `test that invoke with false completes the operation successfully when sync records should be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(true)
            underTest(false)
            verify(cameraUploadRepository,
                times(1)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(1)).saveShouldClearCamSyncRecords(false)
        }

    @Test
    fun `test that invoke with false completes the operation successfully when sync records should not be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(false)
            underTest(false)
            verify(cameraUploadRepository,
                times(0)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(0)).saveShouldClearCamSyncRecords(false)
        }
}