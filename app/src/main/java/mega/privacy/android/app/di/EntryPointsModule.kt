package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.facade.CacheFolderFacade
import mega.privacy.android.app.data.gateway.CacheFolderGateway

@Module
@InstallIn(SingletonComponent::class)
class EntryPointsModule {

    @Provides
    fun provideCacheFolderGateway(cacheFolderFacade: CacheFolderFacade): CacheFolderGateway {
        return cacheFolderFacade
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CacheFolderManagerEntryPoint {
        val cacheFolderGateway: CacheFolderGateway
    }
}