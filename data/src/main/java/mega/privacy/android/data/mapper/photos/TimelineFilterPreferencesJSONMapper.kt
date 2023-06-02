package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

internal class TimelineFilterPreferencesJSONMapper @Inject constructor() {

    operator fun invoke(filterPreferences: String?): Map<String, String?>? =
        try {
            val latestFilterPreferences = filterPreferences?.let { JSONObject(it) }
            val androidPreferences =
                latestFilterPreferences?.getJSONObject(TimelinePreferencesJSON.JSON_KEY_ANDROID.value)
            val timelinePreferences =
                androidPreferences?.getJSONObject(TimelinePreferencesJSON.JSON_KEY_TIMELINE.value)
            toMap(timelinePreferences)
        } catch (exception: JSONException) {
            // "android" or "timeline" key might not exist, return null
            null
        }


    private fun toMap(timelinePreferences: JSONObject?): Map<String, String?> {
        with(timelinePreferences) {
            return listOf(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES,
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE,
                TimelinePreferencesJSON.JSON_KEY_LOCATION
            ).associate {
                it.value to this?.getString(it.value)
            }
        }
    }

}