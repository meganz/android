package mega.privacy.android.data.mapper.transfer

import android.app.Notification

/**
 * Creates a Notification for over quota transfers
 */
interface OverQuotaNotificationBuilder {
    /**
     * Creates a Notification for over quota transfers
     */
    suspend operator fun invoke(): Notification
}