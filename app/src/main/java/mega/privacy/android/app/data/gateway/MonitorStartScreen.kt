package mega.privacy.android.app.data.gateway

import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.data.facade.EventBusFacade
import javax.inject.Inject

class MonitorStartScreen @Inject constructor() : EventBusFacade<Int>(
    EventConstants.EVENT_UPDATE_START_SCREEN,
    Int::class.java
)