package mega.privacy.android.app.appstate.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.user.UserCredentials

@Stable
sealed interface AuthState {
    val themeMode: ThemeMode

    data class Loading(
        override val themeMode: ThemeMode,
    ) : AuthState

    data class RequireLogin(
        override val themeMode: ThemeMode,
    ) : AuthState

    data class LoggedIn(
        override val themeMode: ThemeMode,
        val credentials: UserCredentials,
    ) : AuthState
}