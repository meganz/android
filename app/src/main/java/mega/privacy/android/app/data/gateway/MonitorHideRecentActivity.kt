package mega.privacy.android.app.data.gateway

import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.data.facade.EventBusFacade
import javax.inject.Inject

class MonitorHideRecentActivity @Inject constructor() : EventBusFacade<Boolean>(
    EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY,
    Boolean::class.java
)