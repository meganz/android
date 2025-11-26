package mega.privacy.android.app.appstate.content.mapper

import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import javax.inject.Inject

class ScreenPreferenceDestinationMapper @Inject constructor() {
    operator fun invoke(
        startScreenDestinationPreference: StartScreenDestinationPreference?,
    ) = runCatching {
        val preferenceString = startScreenDestinationPreference?.serialisedDestination
        preferenceString?.let {
            Json.Default.decodeFromString(
                NavKeySerializer,
                it
            )
        } as? MainNavItemNavKey
    }.getOrNull()
}