package mega.privacy.android.app.appstate.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.ThemeMode

@Stable
sealed interface AuthState {
    val themeMode: ThemeMode

    data class Loading(
        override val themeMode: ThemeMode,
    ) : AuthState

    data class RequireLogin(
        override val themeMode: ThemeMode,
        val accountBlockedState: BlockedState,
    ) : AuthState

    data class LoggedIn(
        override val themeMode: ThemeMode,
        val session: String,
    ) : AuthState
}