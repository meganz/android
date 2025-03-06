package mega.privacy.android.app.presentation.settings.compose.appearance.model

sealed interface AppearanceSettingsState {

    data class Data(
        val themeMode: String,
        val showHiddenItems: Boolean,
    ) : AppearanceSettingsState

    data object Loading : AppearanceSettingsState

}