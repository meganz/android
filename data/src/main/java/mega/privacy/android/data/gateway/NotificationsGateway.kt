package mega.privacy.android.data.gateway

import nz.mega.sdk.MegaIntegerList
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Notifications gateway interface
 *
 * for now only Promo notifications SDK commands are added here
 */
internal interface NotificationsGateway {

    /**
     * Get list of available notifications for Notification Center
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_NOTIFICATIONS.
     *
     * When onRequestFinish received MegaError::API_OK, valid data in the MegaRequest object is:
     * - MegaRequest::getMegaNotifications - Returns the list of notifications
     *
     * When onRequestFinish errored, the error code associated to the MegaError can be:
     * - MegaError::API_ENOENT - No such notifications exist, and MegaRequest::getMegaNotifications
     *   will return a non-null, empty list.
     * - MegaError::API_EACCESS - No user was logged in.
     * - MegaError::API_EINTERNAL - Received answer could not be read.
     *
     * @param listener MegaRequestListener to track this request
     */
    fun getNotifications(listener: MegaRequestListenerInterface)

    /**
     * Get the list of IDs for enabled notifications
     *
     * You take the ownership of the returned value
     *
     * @return List of IDs for enabled notifications
     */
    suspend fun getEnabledNotifications(): MegaIntegerList?

    /**
     * Set last read notification for Notification Center
     *
     * The type associated with this request is MegaRequest::TYPE_SET_ATTR_USER
     *
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_LAST_READ_NOTIFICATION
     * - MegaRequest::getNumber - Returns the ID to be set as last read
     *
     * Note that any notifications with ID equal to or less than the given one will be marked as seen
     * in Notification Center.
     *
     * @param notificationId ID of the notification to be set as last read. Value `0` is an invalid ID.
     * Passing `0` will clear a previously set last read value.
     * @param listener MegaRequestListener to track this request
     */
    fun setLastReadNotification(
        notificationId: Long,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Get last read notification for Notification Center
     *
     * The type associated with this request is MegaRequest::TYPE_GET_ATTR_USER
     *
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_LAST_READ_NOTIFICATION
     *
     * When onRequestFinish received MegaError::API_OK, valid data in the MegaRequest object is:
     * - MegaRequest::getNumber - Returns the ID of the last read Notification
     * Note that when the ID returned here was `0` it means that no ID was set as last read.
     * Note that the value returned here should be treated like a 32bit unsigned int.
     *
     * @param listener MegaRequestListener to track this request
     */
    fun getLastReadNotificationId(listener: MegaRequestListenerInterface)
}