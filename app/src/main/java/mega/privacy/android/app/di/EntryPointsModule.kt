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

    @Provides
    fun provideCacheFolderGateway(cacheFolderFacade: CacheFolderFacade): CacheFolderGateway {
        return cacheFolderFacade
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CacheFolderManagerEntryPoint {
        val cacheFolderGateway: CacheFolderGateway
    }

    @Suppress("DEPRECATION")
    @Provides
    fun provideDeviceVibrator(@ApplicationContext context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Entry point gateway for @Vibrator object
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VibratorGateway {

        /**
         * Vibrator object
         */
        val vibrator: Vibrator
    }

    /**
     * Entry point gateway for ThreadPoolExecutor
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MegaThreadPoolExecutorEntryPoint {

        /**
         * ThreadPoolExecutor
         */
        val megaThreadPoolExecutor: ThreadPoolExecutor
    }
}