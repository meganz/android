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
}