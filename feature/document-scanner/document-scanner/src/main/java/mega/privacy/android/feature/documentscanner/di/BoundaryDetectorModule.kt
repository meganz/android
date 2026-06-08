package mega.privacy.android.feature.documentscanner.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.documentscanner.data.boundary.DefaultStabilityTracker
import mega.privacy.android.feature.documentscanner.data.boundary.TFLiteBoundaryDetector
import mega.privacy.android.feature.documentscanner.data.model.DownloadingScannerModelProvider
import mega.privacy.android.feature.documentscanner.data.repository.DefaultScannerPreferencesRepository
import mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.boundary.StabilityTracker
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerPreferencesRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt bindings for document boundary detection.
 *
 * Installed in [SingletonComponent] to match [TFLiteBoundaryDetector]'s
 * `@Singleton` scope — the TFLite model is downloaded once per process and
 * the interpreter is reused. [DownloadingScannerModelProvider] is bound here
 * as well so the detector and its model provider share the singleton scope.
 *
 * [DefaultStabilityTracker] carries no scope annotation, so its binding is
 * unscoped: every consumer (one per `ScanSessionViewModel`) receives its own
 * instance, which keeps the per-session corner-drift state from leaking
 * between scan sessions even though the binding lives in the Singleton
 * component. (An unscoped binding is created fresh on each injection
 * regardless of the component it is installed in.)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BoundaryDetectorModule {

    @Binds
    abstract fun bindDocumentBoundaryDetector(
        impl: TFLiteBoundaryDetector,
    ): DocumentBoundaryDetector

    @Binds
    abstract fun bindStabilityTracker(
        impl: DefaultStabilityTracker,
    ): StabilityTracker

    @Binds
    internal abstract fun bindScannerModelProvider(
        impl: DownloadingScannerModelProvider,
    ): ScannerModelProvider

    @Binds
    @Singleton
    internal abstract fun bindScannerPreferencesRepository(
        impl: DefaultScannerPreferencesRepository,
    ): ScannerPreferencesRepository

    companion object {
        /**
         * Standalone HTTP client for the model download. Not used for any other
         * traffic — the read/call budgets are tuned for a single ~93 MB fetch
         * and would be wrong for typical API calls. Keep this binding qualified
         * with [ScannerModelHttpClient] so it can't be reused by accident.
         */
        @Provides
        @Singleton
        @ScannerModelHttpClient
        internal fun provideScannerModelHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.MINUTES)
                .callTimeout(10, TimeUnit.MINUTES)
                .build()
    }
}
