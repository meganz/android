package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.repository.CacheFileRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDisableCameraUploadSettingProcessTest {
    private lateinit var underTest: DisableCameraUploadSettingProcess

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val settingsRepository = mock<SettingsRepository>()
    private val cacheFileRepository = mock<CacheFileRepository>()


    @Before
    fun setUp() {
        underTest = DefaultDisableCameraUploadSettingProcess(
            cameraUploadRepository = cameraUploadRepository,
            settingsRepository = settingsRepository,
            cacheFileRepository = cacheFileRepository
        )

    }

    @Test
    fun `test that invoke with true completes the operation successfully when sync records should be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(true)
            underTest(true)
            verify(cameraUploadRepository, times(1)).resetCUTimestamps(true)
            verify(cacheFileRepository, times(1)).purgeCacheDirectory()
            verify(cameraUploadRepository, times(1)).shouldClearSyncRecords()
            verify(cameraUploadRepository,
                times(1)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(1)).shouldClearSyncRecords(false)
            verify(settingsRepository, times(1)).setEnableCameraUpload(false)
            verify(cameraUploadRepository, times(1)).setSecondaryEnabled(false)
        }

    @Test
    fun `test that invoke with true completes the operation successfully when sync records should not be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(false)
            underTest(true)
            verify(cameraUploadRepository, times(1)).resetCUTimestamps(true)
            verify(cacheFileRepository, times(1)).purgeCacheDirectory()
            verify(cameraUploadRepository, times(1)).shouldClearSyncRecords()
            verify(cameraUploadRepository,
                times(0)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(0)).shouldClearSyncRecords(false)
            verify(settingsRepository, times(1)).setEnableCameraUpload(false)
            verify(cameraUploadRepository, times(1)).setSecondaryEnabled(false)
        }

    @Test
    fun `test that invoke with false completes the operation successfully when sync records should be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(true)
            underTest(false)
            verify(cameraUploadRepository, times(1)).resetCUTimestamps(false)
            verify(cacheFileRepository, times(1)).purgeCacheDirectory()
            verify(cameraUploadRepository, times(1)).shouldClearSyncRecords()
            verify(cameraUploadRepository,
                times(1)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(1)).shouldClearSyncRecords(false)
            verify(settingsRepository, times(1)).setEnableCameraUpload(false)
            verify(cameraUploadRepository, times(1)).setSecondaryEnabled(false)
        }

    @Test
    fun `test that invoke with false completes the operation successfully when sync records should not be cleared`() =
        runTest {
            whenever(cameraUploadRepository.shouldClearSyncRecords()).thenReturn(false)
            underTest(false)
            verify(cameraUploadRepository, times(1)).resetCUTimestamps(false)
            verify(cacheFileRepository, times(1)).purgeCacheDirectory()
            verify(cameraUploadRepository, times(1)).shouldClearSyncRecords()
            verify(cameraUploadRepository,
                times(0)).deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            verify(cameraUploadRepository, times(0)).shouldClearSyncRecords(false)
            verify(settingsRepository, times(1)).setEnableCameraUpload(false)
            verify(cameraUploadRepository, times(1)).setSecondaryEnabled(false)
        }
}