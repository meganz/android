package mega.privacy.android.app.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.facade.CacheFolderFacade
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import java.util.concurrent.ThreadPoolExecutor

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