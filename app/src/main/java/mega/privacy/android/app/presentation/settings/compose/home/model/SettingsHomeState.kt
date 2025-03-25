package mega.privacy.android.app.presentation.settings.compose.home.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.navigation.settings.SettingEntryPoint

/**
 * Settings ui state
 */
@Immutable
sealed interface SettingsHomeState {
    val featureEntryPoints: ImmutableList<SettingEntryPoint>
    val moreEntryPoints: ImmutableList<SettingEntryPoint>

    /**
     * Loading
     */
    data class Loading(
        override val featureEntryPoints: ImmutableList<SettingEntryPoint>,
        override val moreEntryPoints: ImmutableList<SettingEntryPoint>,
    ) : SettingsHomeState

    /**
     * Data
     *
     * @property myAccountState
     */
    data class Data(
        val myAccountState: MyAccountSettingsState,
        override val featureEntryPoints: ImmutableList<SettingEntryPoint>,
        override val moreEntryPoints: ImmutableList<SettingEntryPoint>,
    ) : SettingsHomeState
}