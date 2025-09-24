package mega.privacy.android.app.appstate.global.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize
import mega.privacy.android.domain.entity.ThemeMode

@Stable
sealed interface GlobalState {
    val themeMode: ThemeMode

    @Parcelize
    data class Loading(
        override val themeMode: ThemeMode,
    ) : GlobalState, Parcelable

    @Parcelize
    data class RequireLogin(
        override val themeMode: ThemeMode,
        val accountBlockedState: BlockedState,
    ) : GlobalState, Parcelable

    @Parcelize
    data class LoggedIn(
        override val themeMode: ThemeMode,
        val session: String,
    ) : GlobalState, Parcelable
}