package mega.privacy.android.data.preferences

import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.preferences.migration.CameraUploadsSettingsPreferenceDataStoreMigration
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class CameraUploadsSettingsPreferenceDataStoreMigrationTest {

    private lateinit var underTest: CameraUploadsSettingsPreferenceDataStoreMigration

    private val databaseHandler = mock<DatabaseHandler>()
    private val dataStore = mock<CameraUploadsSettingsPreferenceDataStore>()

    @BeforeEach
    internal fun setUp() {
        underTest = CameraUploadsSettingsPreferenceDataStoreMigration(
            databaseHandler = databaseHandler,
            cameraUploadsSettingsPreferenceDataStoreFactory = mock {
                on { invoke(anyOrNull()) }.thenReturn(
                    dataStore
                )
            },
        )
    }

    @Test
    internal fun `test that should migrate returns true if no keys exist`() = runTest {
        val currentData = mock<Preferences> { on { asMap() }.thenReturn(emptyMap()) }
        val actual = underTest.shouldMigrate(currentData)
        Truth.assertThat(actual).isTrue()
    }

    @Test
    internal fun `test that default values are set if no preferences exist`() = runTest {
        databaseHandler.stub {
            on { preferences }.thenReturn(null)
        }

        underTest.migrate(mock())

        verify(dataStore).setCameraUploadsEnabled(false)
        verify(dataStore).setMediaUploadsEnabled(false)
        verify(dataStore).setCameraUploadsHandle(null)
        verify(dataStore).setMediaUploadsHandle(null)
        verify(dataStore).setCameraUploadsLocalPath(null)
        verify(dataStore).setMediaUploadsLocalPath(null)
        verify(dataStore).setLocationTagsEnabled(false)
        verify(dataStore).setUploadVideoQuality(VideoQuality.ORIGINAL.value)
        verify(dataStore).setChargingRequiredForVideoCompression(true)
        verify(dataStore).setUploadFileNamesKept(true)
        verify(dataStore).setVideoCompressionSizeLimit(200)
        verify(dataStore).setFileUploadOption(UploadOption.PHOTOS.position)
        verify(dataStore).setPhotoTimeStamp(0L)
        verify(dataStore).setVideoTimeStamp(0L)
        verify(dataStore).setMediaUploadsPhotoTimeStamp(0L)
        verify(dataStore).setMediaUploadsVideoTimeStamp(0L)
        verify(dataStore).setUploadsByWifi(false)
    }

    @Test
    internal fun `test that existing values are transferred if they exist`() = runTest {
        val expectedCameraUploadsEnabled = true
        val expectedMediaUploadsEnabled = true
        val expectedCameraUploadsHandle = 1234L
        val expectedMediaUploadsHandle = 5678L
        val expectedCameraUploadsLocalPath = "/path/to/CU"
        val expectedMediaUploadsLocalPath = "/path/to/MU"
        val expectedLocationTagsEnabled = true
        val expectedUploadVideoQuality = VideoQuality.HIGH.value
        val expectedUploadFileNamesKept = true
        val expectedChargingRequiredForVideoCompression = true
        val expectedVideoCompressionSizeLimit = 400
        val expectedFileUploadOption = UploadOption.PHOTOS_AND_VIDEOS.position
        val expectedPhotoTimeStamp = 1234L
        val expectedVideoTimeStamp = 5678L
        val expectedMediaUploadsPhotoTimeStamp = 1234L
        val expectedMediaUploadsVideoTimeStamp = 5678L
        val expectedUploadsByWifi = true

        val megaPreferences = mock<MegaPreferences> {
            on { camSyncEnabled }.thenReturn(expectedCameraUploadsEnabled.toString())
            on { secondaryMediaFolderEnabled }.thenReturn(expectedMediaUploadsEnabled.toString())
            on { camSyncHandle }.thenReturn(expectedCameraUploadsHandle.toString())
            on { megaHandleSecondaryFolder }.thenReturn(expectedMediaUploadsHandle.toString())
            on { camSyncLocalPath }.thenReturn(expectedCameraUploadsLocalPath)
            on { localPathSecondaryFolder }.thenReturn(expectedMediaUploadsLocalPath)
            on { removeGPS }.thenReturn(expectedLocationTagsEnabled.toString())
            on { uploadVideoQuality }.thenReturn(expectedUploadVideoQuality.toString())
            on { keepFileNames }.thenReturn(expectedUploadFileNamesKept.toString())
            on { conversionOnCharging }.thenReturn(expectedChargingRequiredForVideoCompression.toString())
            on { chargingOnSize }.thenReturn(expectedVideoCompressionSizeLimit.toString())
            on { camSyncFileUpload }.thenReturn(expectedFileUploadOption.toString())
            on { camSyncTimeStamp }.thenReturn(expectedPhotoTimeStamp.toString())
            on { camVideoSyncTimeStamp }.thenReturn(expectedVideoTimeStamp.toString())
            on { secSyncTimeStamp }.thenReturn(expectedMediaUploadsPhotoTimeStamp.toString())
            on { secVideoSyncTimeStamp }.thenReturn(expectedMediaUploadsVideoTimeStamp.toString())
            on { camSyncWifi }.thenReturn(expectedUploadsByWifi.toString())
        }

        databaseHandler.stub {
            on { preferences }.thenReturn(megaPreferences)
        }

        underTest.migrate(mock())

        verify(dataStore).setCameraUploadsEnabled(expectedCameraUploadsEnabled)
        verify(dataStore).setMediaUploadsEnabled(expectedMediaUploadsEnabled)
        verify(dataStore).setCameraUploadsHandle(expectedCameraUploadsHandle)
        verify(dataStore).setMediaUploadsHandle(expectedMediaUploadsHandle)
        verify(dataStore).setCameraUploadsLocalPath(expectedCameraUploadsLocalPath)
        verify(dataStore).setMediaUploadsLocalPath(expectedMediaUploadsLocalPath)
        verify(dataStore).setLocationTagsEnabled(expectedLocationTagsEnabled)
        verify(dataStore).setUploadVideoQuality(expectedUploadVideoQuality)
        verify(dataStore).setUploadFileNamesKept(expectedUploadFileNamesKept)
        verify(dataStore).setChargingRequiredForVideoCompression(
            expectedChargingRequiredForVideoCompression
        )
        verify(dataStore).setVideoCompressionSizeLimit(expectedVideoCompressionSizeLimit)
        verify(dataStore).setFileUploadOption(expectedFileUploadOption)
        verify(dataStore).setPhotoTimeStamp(expectedPhotoTimeStamp)
        verify(dataStore).setVideoTimeStamp(expectedVideoTimeStamp)
        verify(dataStore).setMediaUploadsPhotoTimeStamp(expectedMediaUploadsPhotoTimeStamp)
        verify(dataStore).setMediaUploadsVideoTimeStamp(expectedMediaUploadsVideoTimeStamp)
        verify(dataStore).setUploadsByWifi(expectedUploadsByWifi)
    }
}
