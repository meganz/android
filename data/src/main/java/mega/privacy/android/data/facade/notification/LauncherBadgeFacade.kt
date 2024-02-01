package mega.privacy.android.data.facade.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.leolin.shortcutbadger.ShortcutBadger
import mega.privacy.android.data.gateway.notification.LauncherBadgeGateway
import javax.inject.Inject

/**
 * Launcher badge facade
 *
 * @property context
 */
class LauncherBadgeFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : LauncherBadgeGateway {
    override fun setLauncherBadgeCount(count: Int) {
        ShortcutBadger.applyCount(context, count)
    }
}