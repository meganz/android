package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable


@Serializable
data object Secondary : NavKey

@Serializable
data class ExampleLegacyScreen(
    val content: String
) : NavKey

@Serializable
data object ExampleLegacyResultScreen : NavKey {
    const val RESULT_KEY = "ExampleLegacyResultScreenResultKey"
}