package mega.privacy.android.app.di.pushes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.usecase.GetPushToken

/**
 * Pushes use cases module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushesModule {

    companion object {
        @Provides
        fun provideGetPushToken(pushesRepository: PushesRepository): GetPushToken =
            GetPushToken(pushesRepository::getPushToken)
    }
}