package mega.privacy.android.app.data.gateway

import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.data.facade.EventBusFacade
import javax.inject.Inject

/**
 * Monitor start screen facade
 *
 * Emits events whenever the start screen preference is changed
 */
class MonitorStartScreenFacade @Inject constructor() : EventBusFacade<Int>(
    EventConstants.EVENT_UPDATE_START_SCREEN,
    Int::class.java
)