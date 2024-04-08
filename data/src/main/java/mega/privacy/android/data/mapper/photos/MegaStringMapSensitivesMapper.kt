package mega.privacy.android.data.mapper.photos

import mega.privacy.android.data.extensions.decodeBase64
import mega.privacy.android.data.extensions.encodeBase64
import mega.privacy.android.data.extensions.getValueFor
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import nz.mega.sdk.MegaStringMap
import org.json.JSONObject
import javax.inject.Inject

internal class MegaStringMapSensitivesMapper @Inject constructor() {
    operator fun invoke(prefs: MegaStringMap?, data: Map<String, Any>): MegaStringMap {
        val json = try {
            prefs?.getValueFor(TimelinePreferencesJSON.JSON_KEY_CONTENT_CONSUMPTION.value)
                ?.decodeBase64()
                ?.let { JSONObject(it) }
                ?: JSONObject()
        } catch (e: Exception) {
            JSONObject()
        }
        json.put(TimelinePreferencesJSON.JSON_SENSITIVES.value, JSONObject(data))

        val newPrefs = prefs ?: MegaStringMap.createInstance()
        newPrefs[TimelinePreferencesJSON.JSON_KEY_CONTENT_CONSUMPTION.value] =
            json.toString().encodeBase64()

        return newPrefs
    }
}
