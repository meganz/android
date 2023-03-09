package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.offline.DefaultSetNodeAvailableOffline
import mega.privacy.android.app.domain.usecase.offline.SetNodeAvailableOffline

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
    abstract fun bindSetNodeAvailableOffline(impl: DefaultSetNodeAvailableOffline): SetNodeAvailableOffline
}