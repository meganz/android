package mega.privacy.android.app.presentation.settings.camerauploads.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Enum class for Camera Uploads that defines the list of connection options for uploading content
 *
 * @property textRes The String resource
 * @property position The position in the list
 */
enum class UploadConnectionType(@StringRes val textRes: Int, val position: Int) {

    /**
     * User can upload content either through Wi-Fi or Mobile Data
     */
    WIFI_OR_MOBILE_DATA(R.string.cam_sync_data, 0),

    /**
     * User can upload content only through Wi-Fi
     */
    WIFI(R.string.cam_sync_wifi, 1),
}