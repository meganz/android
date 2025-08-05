package mega.privacy.android.app.appstate.mapper

import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import javax.inject.Inject

class ScreenPreferenceDestinationMapper @Inject constructor() {
    operator fun invoke(
        startScreenDestinationPreference: StartScreenDestinationPreference?,
    ) = runCatching {
        val preferenceString = startScreenDestinationPreference?.serialisedDestination
        preferenceString?.let { Json.decodeFromString(NavKeySerializer, it) }
    }.getOrNull()
}
