package mega.privacy.android.feature.documentscanner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.documentscanner.data.boundary.DefaultStabilityTracker
import mega.privacy.android.feature.documentscanner.data.boundary.TFLiteBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.boundary.StabilityTracker

/**
 * Hilt bindings for document boundary detection.
 *
 * Installed in [SingletonComponent] to match [TFLiteBoundaryDetector]'s
 * `@Singleton` scope — the TFLite model is loaded once per process and reused.
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
}
