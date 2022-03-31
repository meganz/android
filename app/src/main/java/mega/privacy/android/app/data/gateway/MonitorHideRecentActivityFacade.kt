package mega.privacy.android.app.data.gateway

import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.data.facade.EventBusFacade
import javax.inject.Inject

/**
 * Monitor hide recent activity facade
 *
 * Emits events whenever the hide recent activity preference is changed
 */
class MonitorHideRecentActivityFacade @Inject constructor() : EventBusFacade<Boolean>(
    EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY,
    Boolean::class.java
)