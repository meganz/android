package mega.privacy.android.data.mapper.analytics

import com.google.gson.GsonBuilder
import mega.privacy.android.domain.entity.analytics.AnalyticsEvent
import javax.inject.Inject

/**
 * Analytics event message mapper
 *
 * @property gsonBuilder
 * @constructor Create empty Analytics event message mapper
 */
class AnalyticsEventMessageMapper @Inject constructor(
    private val gsonBuilder: GsonBuilder,
) {
    private val gson = gsonBuilder.serializeNulls().create()

    /**
     * Invoke
     *
     * @param event
     * @return
     */
    operator fun invoke(event: AnalyticsEvent): String = gson.toJson(event.data())
}
