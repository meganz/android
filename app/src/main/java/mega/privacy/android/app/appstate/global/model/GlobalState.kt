package mega.privacy.android.app.appstate.global.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize
import mega.privacy.android.domain.entity.ThemeMode

@Stable
sealed interface GlobalState {
    val themeMode: ThemeMode
    val isConnected: Boolean

    @Parcelize
    data class Loading(
        override val themeMode: ThemeMode,
        override val isConnected: Boolean = true,
    ) : GlobalState, Parcelable

    @Parcelize
    data class RequireLogin(
        override val themeMode: ThemeMode,
        val accountBlockedState: BlockedState,
        override val isConnected: Boolean,
    ) : GlobalState, Parcelable

    @Parcelize
    data class LoggedIn(
        override val themeMode: ThemeMode,
        val session: String,
        override val isConnected: Boolean,
    ) : GlobalState, Parcelable
}