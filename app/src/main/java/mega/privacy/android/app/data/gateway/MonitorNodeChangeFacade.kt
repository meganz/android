package mega.privacy.android.app.data.gateway

import mega.privacy.android.app.data.facade.EventBusFacade
import mega.privacy.android.app.utils.Constants
import javax.inject.Inject

/**
 * Create the event that monitor node change
 */
class MonitorNodeChangeFacade @Inject constructor(): EventBusFacade<Boolean>(
    Constants.EVENT_NODES_CHANGE,
    Boolean::class.java
)