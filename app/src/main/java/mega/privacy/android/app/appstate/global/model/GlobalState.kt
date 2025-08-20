package mega.privacy.android.app.appstate.global.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.ThemeMode

@Stable
sealed interface GlobalState {
    val themeMode: ThemeMode

    data class Loading(
        override val themeMode: ThemeMode,
    ) : GlobalState

    data class RequireLogin(
        override val themeMode: ThemeMode,
        val accountBlockedState: BlockedState,
    ) : GlobalState

    data class LoggedIn(
        override val themeMode: ThemeMode,
        val session: String,
    ) : GlobalState
}