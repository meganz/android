package mega.privacy.android.analytics.event.link

import mega.privacy.android.analytics.event.GeneralInfo

/**
 * Class to track all link screen info here
 */
class LinkScreenInfoAnalytics(
    override val uniqueIdentifier: Int,
    override val name: String,
    override val info: String?
) : GeneralInfo