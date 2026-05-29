package mega.privacy.android.app.presentation.meeting.chat.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Internal NavKey used by [mega.privacy.android.app.presentation.meeting.chat.ChatFragment] as the
 * root of its [mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold] so the
 * chat's existing legacy navigation graph runs inside a Navigation3-aware host.
 */
@Serializable
internal data object ChatLegacyContainerNavKey : NavKey
