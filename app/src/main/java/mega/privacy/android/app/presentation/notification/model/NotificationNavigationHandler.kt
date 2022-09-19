package mega.privacy.android.app.presentation.notification.model

/**
 * Notification navigation handler
 *
 */
interface NotificationNavigationHandler {
    /**
     * Navigate to shared node
     *
     * @param nodeId
     * @param childNodes
     */
    fun navigateToSharedNode(nodeId: Long, childNodes: LongArray?)

    /**
     * Navigate to my account
     *
     */
    fun navigateToMyAccount()

    /**
     * Navigate to contact info
     *
     * @param email
     */
    fun navigateToContactInfo(email: String)

    /**
     * Navigate to contact requests
     *
     */
    fun navigateToContactRequests()
}