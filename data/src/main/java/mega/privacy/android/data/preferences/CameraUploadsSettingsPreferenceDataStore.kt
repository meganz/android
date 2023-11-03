package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway

internal const val cameraUploadsSettingsPreferenceDataStoreName = "cameraUploadsSettingsDataStore"

/**
 * CameraUploads Setting sPreference DataStore
 */
internal class CameraUploadsSettingsPreferenceDataStore(
    private val getPreferenceFlow: () -> Flow<Preferences>,
    private val editPreferences: suspend (suspend (MutablePreferences) -> Unit) -> Preferences,
    private val encryptData: EncryptData,
    private val decryptData: DecryptData,
) : CameraUploadsSettingsPreferenceGateway {

    constructor(
        dataStore: DataStore<Preferences>,
        encryptData: EncryptData,
        decryptData: DecryptData,
    ) : this(
        getPreferenceFlow = dataStore::data,
        editPreferences = dataStore::edit,
        encryptData = encryptData,
        decryptData = decryptData,
    )

    private val cameraUploadsEnabledKey = stringPreferencesKey("cameraUploadsEnabledKey")
    private val mediaUploadsEnabledKey = stringPreferencesKey("mediaUploadsEnabledKey")
    private val cameraUploadsHandleKey = stringPreferencesKey("cameraUploadsHandleKey")
    private val mediaUploadsHandleKey = stringPreferencesKey("mediaUploadsHandleKey")
    private val cameraUploadsLocalPathKey = stringPreferencesKey("cameraUploadsLocalPathKey")
    private val mediaUploadsLocalPathKey = stringPreferencesKey("mediaUploadsLocalPathKey")
    private val locationTagsEnabledKey = stringPreferencesKey("locationTagsEnabledKey")
    private val uploadVideoQualityKey = stringPreferencesKey("uploadVideoQualityKey")
    private val uploadFileNamesKeptKey = stringPreferencesKey("uploadFileNamesKeptKey")
    private val chargingRequiredForVideoCompressionKey =
        stringPreferencesKey("chargingRequiredForVideoCompressionKey")
    private val videoCompressionSizeLimitKey =
        stringPreferencesKey("videoCompressionSizeLimitKey")
    private val fileUploadOptionKey =
        stringPreferencesKey("fileUploadOptionKey")
    private val photoTimeStampKey = stringPreferencesKey("photoTimeStampKey")
    private val videoTimeStampKey = stringPreferencesKey("videoTimeStampKey")
    private val mediaUploadsPhotoTimeStampKey =
        stringPreferencesKey("mediaUploadsPhotoTimeStampKey")
    private val mediaUploadsVideoTimeStampKey =
        stringPreferencesKey("mediaUploadsVideoTimeStampKey")
    private val uploadByWifiKey = stringPreferencesKey("uploadByWifiKey")


    override suspend fun isCameraUploadsEnabled(): Boolean? {
        return getPreferenceFlow().monitor(cameraUploadsEnabledKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }.firstOrNull()
    }

    override suspend fun isMediaUploadsEnabled(): Boolean? {
        return getPreferenceFlow().monitor(mediaUploadsEnabledKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }.firstOrNull()
    }

    override suspend fun getCameraUploadsHandle(): Long? {
        return getPreferenceFlow().monitor(cameraUploadsHandleKey)
            .map { decryptData(it)?.toLongOrNull() }.firstOrNull()
    }

    override suspend fun getMediaUploadsHandle(): Long? {
        return getPreferenceFlow().monitor(mediaUploadsHandleKey)
            .map { decryptData(it)?.toLongOrNull() }.firstOrNull()
    }

    override suspend fun setMediaUploadsHandle(handle: Long?) {
        val encryptedValue = encryptData(handle?.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(mediaUploadsHandleKey)
            } else {
                it[mediaUploadsHandleKey] = encryptedValue
            }
        }
    }

    override suspend fun setCameraUploadsHandle(handle: Long?) {
        val encryptedValue = encryptData(handle?.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(cameraUploadsHandleKey)
            } else {
                it[cameraUploadsHandleKey] = encryptedValue
            }
        }
    }

    override suspend fun getCameraUploadsLocalPath(): String? {
        return getPreferenceFlow().monitor(cameraUploadsLocalPathKey)
            .map { decryptData(it) }.firstOrNull()
    }

    override suspend fun setCameraUploadsLocalPath(path: String?) {
        val encryptedValue = encryptData(path)
        editPreferences {
            if (encryptedValue == null) {
                it.remove(cameraUploadsLocalPathKey)
            } else {
                it[cameraUploadsLocalPathKey] = encryptedValue
            }
        }
    }

    override suspend fun getMediaUploadsLocalPath(): String? {
        return getPreferenceFlow().monitor(mediaUploadsLocalPathKey)
            .map { decryptData(it) }.firstOrNull()
    }

    override suspend fun setMediaUploadsLocalPath(path: String?) {
        val encryptedValue = encryptData(path)
        editPreferences {
            if (encryptedValue == null) {
                it.remove(mediaUploadsLocalPathKey)
            } else {
                it[mediaUploadsLocalPathKey] = encryptedValue
            }
        }
    }

    override suspend fun setCameraUploadsEnabled(isEnabled: Boolean) {
        val encryptedValue = encryptData(isEnabled.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(cameraUploadsEnabledKey)
            } else {
                it[cameraUploadsEnabledKey] = encryptedValue
            }
        }
    }

    override suspend fun setMediaUploadsEnabled(isEnabled: Boolean) {
        val encryptedValue = encryptData(isEnabled.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(mediaUploadsEnabledKey)
            } else {
                it[mediaUploadsEnabledKey] = encryptedValue
            }
        }
    }

    override suspend fun areLocationTagsEnabled(): Boolean? {
        return getPreferenceFlow().monitor(locationTagsEnabledKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }.firstOrNull()
    }

    override suspend fun setLocationTagsEnabled(isEnabled: Boolean) {
        val encryptedValue = encryptData(isEnabled.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(locationTagsEnabledKey)
            } else {
                it[locationTagsEnabledKey] = encryptedValue
            }
        }
    }

    override suspend fun getUploadVideoQuality(): Int? {
        return getPreferenceFlow().monitor(uploadVideoQualityKey)
            .map { decryptData(it)?.toIntOrNull() }.firstOrNull()
    }

    override suspend fun setUploadVideoQuality(quality: Int) {
        val encryptedValue = encryptData(quality.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(uploadVideoQualityKey)
            } else {
                it[uploadVideoQualityKey] = encryptedValue
            }
        }
    }

    override suspend fun setUploadFileNamesKept(keepFileNames: Boolean) {
        val encryptedValue = encryptData(keepFileNames.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(uploadFileNamesKeptKey)
            } else {
                it[uploadFileNamesKeptKey] = encryptedValue
            }
        }
    }

    override suspend fun areUploadFileNamesKept(): Boolean? {
        return getPreferenceFlow().monitor(uploadFileNamesKeptKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }.firstOrNull()
    }

    override suspend fun isChargingRequiredForVideoCompression(): Boolean? {
        return getPreferenceFlow().monitor(chargingRequiredForVideoCompressionKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }.firstOrNull()
    }

    override suspend fun setChargingRequiredForVideoCompression(chargingRequired: Boolean) {
        val encryptedValue = encryptData(chargingRequired.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(chargingRequiredForVideoCompressionKey)
            } else {
                it[chargingRequiredForVideoCompressionKey] = encryptedValue
            }
        }
    }

    override suspend fun getVideoCompressionSizeLimit(): Int? {
        return getPreferenceFlow().monitor(videoCompressionSizeLimitKey)
            .map { decryptData(it)?.toIntOrNull() }.firstOrNull()
    }

    override suspend fun setVideoCompressionSizeLimit(size: Int) {
        val encryptedValue = encryptData(size.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(videoCompressionSizeLimitKey)
            } else {
                it[videoCompressionSizeLimitKey] = encryptedValue
            }
        }
    }

    override suspend fun getFileUploadOption(): Int? {
        return getPreferenceFlow().monitor(fileUploadOptionKey)
            .map { decryptData(it)?.toIntOrNull() }.firstOrNull()
    }

    override suspend fun setFileUploadOption(uploadOption: Int) {
        val encryptedValue = encryptData(uploadOption.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(fileUploadOptionKey)
            } else {
                it[fileUploadOptionKey] = encryptedValue
            }
        }
    }

    override suspend fun getPhotoTimeStamp(): Long? {
        return getPreferenceFlow().monitor(photoTimeStampKey)
            .map { decryptData(it)?.toLongOrNull() }.firstOrNull()
    }

    override suspend fun setPhotoTimeStamp(timeStamp: Long) {
        val encryptedValue = encryptData(timeStamp.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(photoTimeStampKey)
            } else {
                it[photoTimeStampKey] = encryptedValue
            }
        }
    }

    override suspend fun getVideoTimeStamp(): Long? {
        return getPreferenceFlow().monitor(videoTimeStampKey)
            .map { decryptData(it)?.toLongOrNull() }.firstOrNull()
    }

    override suspend fun setVideoTimeStamp(timeStamp: Long) {
        val encryptedValue = encryptData(timeStamp.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(videoTimeStampKey)
            } else {
                it[videoTimeStampKey] = encryptedValue
            }
        }
    }

    override suspend fun getMediaUploadsPhotoTimeStamp(): Long? {
        return getPreferenceFlow().monitor(mediaUploadsPhotoTimeStampKey)
            .map { decryptData(it)?.toLongOrNull() }.firstOrNull()
    }

    override suspend fun setMediaUploadsPhotoTimeStamp(timeStamp: Long) {
        val encryptedValue = encryptData(timeStamp.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(mediaUploadsPhotoTimeStampKey)
            } else {
                it[mediaUploadsPhotoTimeStampKey] = encryptedValue
            }
        }
    }

    override suspend fun getMediaUploadsVideoTimeStamp(): Long? {
        return getPreferenceFlow().monitor(mediaUploadsVideoTimeStampKey)
            .map { decryptData(it)?.toLongOrNull() }.firstOrNull()
    }

    override suspend fun setMediaUploadsVideoTimeStamp(timeStamp: Long) {
        val encryptedValue = encryptData(timeStamp.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(mediaUploadsVideoTimeStampKey)
            } else {
                it[mediaUploadsVideoTimeStampKey] = encryptedValue
            }
        }
    }

    override suspend fun isUploadByWifi(): Boolean? {
        return getPreferenceFlow().monitor(uploadByWifiKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }.firstOrNull()
    }

    override suspend fun setUploadsByWifi(wifiOnly: Boolean) {
        val encryptedValue = encryptData(wifiOnly.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(uploadByWifiKey)
            } else {
                it[uploadByWifiKey] = encryptedValue
            }
        }
    }

    override suspend fun clear() {
        editPreferences {
            it.clear()
        }
    }
}
