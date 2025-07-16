package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable


@Serializable
data object Secondary

@Serializable
data class ExampleLegacyScreen(
    val content: String
)

@Serializable
data object ExampleLegacyResultScreen{
    const val RESULT_KEY = "ExampleLegacyResultScreenResultKey"
}