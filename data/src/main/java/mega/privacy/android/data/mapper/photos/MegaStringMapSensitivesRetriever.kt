package mega.privacy.android.data.mapper.photos

import mega.privacy.android.data.extensions.decodeBase64
import mega.privacy.android.data.extensions.getValueFor
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import nz.mega.sdk.MegaStringMap
import org.json.JSONObject
import javax.inject.Inject

internal class MegaStringMapSensitivesRetriever @Inject constructor() {
    operator fun invoke(prefs: MegaStringMap?): Boolean {
        return try {
            prefs?.getValueFor(TimelinePreferencesJSON.JSON_KEY_CONTENT_CONSUMPTION.value)
                ?.decodeBase64()
                ?.let { JSONObject(it) }
                ?.getJSONObject(TimelinePreferencesJSON.JSON_SENSITIVES.value)
                ?.getBoolean(TimelinePreferencesJSON.JSON_SENSITIVES_ONBOARDED.value) ?: false
        } catch (e: Exception) {
            false
        }
    }
}
