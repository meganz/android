package mega.privacy.android.app.initializer

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Notification channels initializer
 */
class NotificationChannelsInitializer : Initializer<Unit> {
    /**
     * Create
     */
    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            NotificationChannelsInitializerEntryPoint::class.java
        )

        //Recreating an existing notification channel with its original values performs no operation, so it's safe to call this code when starting an app.
        //source: https://developer.android.com/develop/ui/views/notifications/channels#CreateChannel
        entryPoint.getNotificationManager()
            .createNotificationChannelsCompat(entryPoint.getChannels().toList())
    }

    /**
     * dependencies
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    /**
     * Notification channels initializer entry point
     */
    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface NotificationChannelsInitializerEntryPoint {
        /**
         * Notification manager
         */
        fun getNotificationManager(): NotificationManagerCompat

        /**
         * Injected channels to be created
         */
        fun getChannels(): Set<NotificationChannelCompat>
    }
}