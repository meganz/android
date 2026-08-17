package mega.privacy.android.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.PushNotificationSettingManagement

/**
 * Entry point to resolve the chat singletons in classes that cannot use constructor
 * or field injection (legacy adapters, listeners and hand-instantiated classes).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatComponentsEntryPoint {

    /**
     * The [ChatManagement] instance.
     */
    fun chatManagement(): ChatManagement

    /**
     * The [PushNotificationSettingManagement] instance.
     */
    fun pushNotificationSettingManagement(): PushNotificationSettingManagement
}
