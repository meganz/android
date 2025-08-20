package mega.privacy.android.app.presentation.settings.startscreen.mapper

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import mega.privacy.android.app.appstate.content.mapper.NavKeySerializer
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import javax.inject.Inject

class StartScreenDestinationPreferenceNavKeyMapper @Inject constructor() {
    operator fun invoke(
        navKey: NavKey?,
    ) = runCatching {
        navKey?.let {
            Json.encodeToString(
                NavKeySerializer,
                it
            )
        }
    }.mapCatching { serialisedDestination ->
        serialisedDestination?.let { StartScreenDestinationPreference(it) }
    }.getOrNull()
}