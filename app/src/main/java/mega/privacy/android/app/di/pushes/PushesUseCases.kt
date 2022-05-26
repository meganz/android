package mega.privacy.android.app.di.pushes

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.*

/**
 * Pushes use cases module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushesUseCases {

    @Binds
    abstract fun bindGetPushToken(useCase: DefaultGetPushToken): GetPushToken

    @Binds
    abstract fun bindRegisterPushNotification(useCase: DefaultRegisterPushNotifications): RegisterPushNotifications

    @Binds
    abstract fun bindSetPushToken(useCase: DefaultSetPushToken): SetPushToken

    @Binds
    abstract fun bindPushReceived(useCase: DefaultPushReceived): PushReceived
}