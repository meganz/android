package mega.privacy.android.domain.entity.photos

/**
 * Enum class for the Timeline Preferences JSON value and key
 *
 * @property value
 */
enum class TimelinePreferencesJSON(val value: String) {

    JSON_KEY_CONTENT_CONSUMPTION("cc"),

    JSON_KEY_ANDROID("android"),

    JSON_KEY_TIMELINE("timeline"),

    JSON_KEY_REMEMBER_PREFERENCES("rememberPreferences"),

    JSON_KEY_MEDIA_TYPE("mediaType"),

    JSON_KEY_LOCATION("location"),

    JSON_VAL_MEDIA_TYPE_ALL_MEDIA("allMedia"),

    JSON_VAL_MEDIA_TYPE_IMAGES("images"),

    JSON_VAL_MEDIA_TYPE_VIDEOS("videos"),

    JSON_VAL_LOCATION_ALL_LOCATION("allLocation"),

    JSON_VAL_LOCATION_CLOUD_DRIVE("cloudDrive"),

    JSON_VAL_LOCATION_CAMERA_UPLOAD("cameraUploads"),
}
