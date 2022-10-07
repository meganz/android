package mega.privacy.android.app.di

import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.gateway.CacheFolderGateway

@Module
@InstallIn(SingletonComponent::class)
/**
 * Module to define custom entry points
 */
class EntryPointsModule {

    /**
     * Entry point for @CacheFolderGateway
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CacheFolderManagerEntryPoint {

        /**
         * @CacheFolderGateway
         */
        val cacheFolderGateway: CacheFolderGateway
    }
}