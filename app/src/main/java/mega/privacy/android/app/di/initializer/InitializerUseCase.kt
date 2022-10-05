package mega.privacy.android.app.di.initializer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.CheckAppUpdate
import mega.privacy.android.domain.usecase.DefaultCheckAppUpdate

/**
 * Initializer use case
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InitializerUseCase {
    /**
     * Provide check app update
     *
     */
    @Binds
    abstract fun provideCheckAppUpdate(implementation: DefaultCheckAppUpdate): CheckAppUpdate
}