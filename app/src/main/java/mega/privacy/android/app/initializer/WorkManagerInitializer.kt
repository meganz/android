package mega.privacy.android.app.initializer

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * WorkManager initializer
 */
class WorkManagerInitializer : Initializer<WorkManager> {

    /**
     * Create
     */
    override fun create(context: Context): WorkManager {
        val workerFactory = getWorkerFactory(appContext = context.applicationContext)
        val configuration = Configuration.Builder().setWorkerFactory(workerFactory).build()
        WorkManager.initialize(context, configuration)
        return WorkManager.getInstance(context)
    }

    /**
     * Dependencies
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun getWorkerFactory(appContext: Context): HiltWorkerFactory {
        val workManagerEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WorkManagerInitializerEntryPoint::class.java
        )
        return workManagerEntryPoint.hiltWorkerFactory()
    }

    /**
     * WorkManagerInitializer entry point
     */
    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface WorkManagerInitializerEntryPoint {
        /**
         * HiltWorkerFactory
         */
        fun hiltWorkerFactory(): HiltWorkerFactory
    }
}
