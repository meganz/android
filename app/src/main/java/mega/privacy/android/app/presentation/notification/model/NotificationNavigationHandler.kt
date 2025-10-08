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
     */
    fun navigateToSharedNode(nodeId: Long)

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

    /**
     * Navigate to chat section
     *
     * @param chatId
     */
    fun moveToChatSection(chatId: Long)
}