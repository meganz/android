package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.navigation.MegaNavigatorEntryPoint

/**
 * Open sent requests
 */
internal fun openSentRequests(context: Context) {
    EntryPointAccessors.fromApplication(context, MegaNavigatorEntryPoint::class.java)
        .megaNavigator
        .openSentRequests(context)
}
