package mega.privacy.android.app.workmanager

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject

/**
 * Builds the WorkManager [Configuration] used for on-demand initialization.
 *
 * The [Configuration.Provider] interface itself must stay on the Application class — WorkManager's
 * on-demand initialization contract resolves the configuration through it — so the Application
 * delegates here to keep the construction injectable.
 */
class WorkManagerConfigurationProvider @Inject constructor(
    private val hiltWorkerFactory: HiltWorkerFactory,
) : Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
}
