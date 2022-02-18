package mega.privacy.android.app.data.gateway

import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.data.facade.EventBusFacade
import javax.inject.Inject

class MonitorNetworkConnectivityChange @Inject constructor() : EventBusFacade<Boolean>(
    EventConstants.EVENT_NETWORK_CHANGE,
    Boolean::class.java
)