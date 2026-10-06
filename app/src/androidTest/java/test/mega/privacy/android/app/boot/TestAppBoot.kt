package mega.privacy.android.app.boot

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.global.initialisation.GlobalInitialiser
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.usecase.setting.BroadcastMiscStateUseCase
import mega.privacy.android.navigation.contract.initialisation.AppCreateInitialiser

/**
 * Boots an instrumented-test process through the SAME initialiser units as production.
 *
 * In production `MegaApplication.onCreate` calls [GlobalInitialiser.onAppCreate] and its process
 * lifecycle `onStart` calls [GlobalInitialiser.onAppStart]. Under the Hilt test application neither
 * runs (there is no `MegaApplication`), so instrumented tests must trigger the same units by hand.
 * Doing that through [GlobalInitialiser] rather than replicating individual boot steps keeps test
 * and production boot converged: a test process is initialised by exactly the production units,
 * in production order.
 *
 * Both [GlobalInitialiser.onAppCreate] and [GlobalInitialiser.onAppStart] are guarded to run once
 * per process, so repeated calls (across tests in the same process) are no-ops.
 */
object TestAppBoot {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface TestAppBootEntryPoint {
        fun globalInitialiser(): GlobalInitialiser
        fun hiltWorkerFactory(): HiltWorkerFactory
        fun broadcastMiscStateUseCase(): BroadcastMiscStateUseCase
    }

    /**
     * Runs the production boot initialiser units in the test process.
     *
     * WorkManager is initialised first: some units' dependency graphs (and the FCM service's) can
     * resolve WorkManager on demand, so it must be ready before [GlobalInitialiser.onAppCreate].
     * [GlobalInitialiser.onAppCreate] then runs on the main thread, as `Application.onCreate` does,
     * because synchronous units such as `AppLifecycleInitialiser` require it.
     *
     * @param exclude names of app-create units to skip, matching [AppCreateInitialiser.name]. Empty
     * by default so tests boot with the full production unit set; add a name only for a unit a test
     * demonstrably cannot tolerate.
     * @param includeAppStart when true (the default) also runs the app-start tier
     * ([GlobalInitialiser.onAppStart]), which launches into the application scope and drives units
     * such as the transfer-events monitor that persists upload progress.
     * @param miscFlagsReady when true (the default) the process is marked as having the SDK's misc
     * flags loaded, as a logged-in session has shortly after start. The real SDK signals this with
     * `EVENT_MISC_FLAGS_READY` through the `MegaApiAndroid` global listener, which the fake gateways
     * never drive; without the mark every API feature-flag check waits out the provider's 2 s
     * timeout. Pass false for a test that sequences the flags-ready state itself.
     */
    fun runCoreInitializers(
        exclude: Set<String> = emptySet(),
        includeAppStart: Boolean = true,
        miscFlagsReady: Boolean = true,
    ) {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val entryPoint = EntryPointAccessors.fromApplication(
            targetContext,
            TestAppBootEntryPoint::class.java,
        )

        WorkManagerTestInitHelper.initializeTestWorkManager(
            targetContext,
            Configuration.Builder().setWorkerFactory(entryPoint.hiltWorkerFactory()).build(),
        )

        val globalInitialiser = entryPoint.globalInitialiser()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            globalInitialiser.onAppCreate { it.name !in exclude }
        }
        if (includeAppStart) {
            globalInitialiser.onAppStart()
        }
        if (miscFlagsReady) {
            runBlocking { entryPoint.broadcastMiscStateUseCase()(MiscLoadedState.FlagsReady) }
        }
    }
}
