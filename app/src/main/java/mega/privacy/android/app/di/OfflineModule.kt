package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.offline.DefaultRemoveAvailableOfflineUseCase
import mega.privacy.android.app.domain.usecase.offline.RemoveAvailableOfflineUseCase

/**
 * binds use-cases related to offline
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class OfflineModule {

    /**
     * binds default implementation
     */
    @Binds
    abstract fun bindSetNodeAvailableOffline(impl: DefaultRemoveAvailableOfflineUseCase): RemoveAvailableOfflineUseCase
}