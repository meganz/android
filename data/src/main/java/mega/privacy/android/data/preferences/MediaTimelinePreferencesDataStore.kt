package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.preferences.MediaTimelinePreferencesGateway
import mega.privacy.android.data.qualifier.MediaTimelinePreferenceDataStore
import javax.inject.Inject

internal const val mediaTimelinePreferenceFileName = "MEDIA_TIMELINE_PREFERENCES_FILE_NAME"

class MediaTimelinePreferencesDataStore @Inject constructor(
    @MediaTimelinePreferenceDataStore private val mediaTimelinePreferenceDataStore: DataStore<Preferences>,
    private val deviceGateway: DeviceGateway,
) : MediaTimelinePreferencesGateway {

    override val cameraUploadShownFlow: Flow<Boolean> = mediaTimelinePreferenceDataStore.data.map {
        it[MEDIA_TIMELINE_CAMERA_UPLOAD_SHOWN_PREF_KEY] ?: false
    }

    override suspend fun setCameraUploadShown() {
        mediaTimelinePreferenceDataStore.edit {
            it[MEDIA_TIMELINE_CAMERA_UPLOAD_SHOWN_PREF_KEY] = true
        }
    }

    override val enableCameraUploadBannerDismissedTimestamp: Flow<Long?> =
        mediaTimelinePreferenceDataStore.data.map {
            it[MEDIA_TIMELINE_ENABLE_CAMERA_UPLOAD_BANNER_DISMISSED_TIMESTAMP_PREF_KEY]
        }

    override suspend fun setEnableCameraUploadBannerDismissedTimestamp() {
        mediaTimelinePreferenceDataStore.edit {
            it[MEDIA_TIMELINE_ENABLE_CAMERA_UPLOAD_BANNER_DISMISSED_TIMESTAMP_PREF_KEY] =
                deviceGateway.now
        }
    }

    override suspend fun resetEnableCameraUploadBannerDismissedTimestamp() {
        mediaTimelinePreferenceDataStore.edit {
            it.remove(MEDIA_TIMELINE_ENABLE_CAMERA_UPLOAD_BANNER_DISMISSED_TIMESTAMP_PREF_KEY)
        }
    }

    companion object {
        private val MEDIA_TIMELINE_CAMERA_UPLOAD_SHOWN_PREF_KEY =
            booleanPreferencesKey("MEDIA_TIMELINE_CAMERA_UPLOAD_SHOWN")
        private val MEDIA_TIMELINE_ENABLE_CAMERA_UPLOAD_BANNER_DISMISSED_TIMESTAMP_PREF_KEY =
            longPreferencesKey("MEDIA_TIMELINE_ENABLE_CAMERA_UPLOAD_BANNER_DISMISSED_TIMESTAMP_PREF_KEY")
    }
}
