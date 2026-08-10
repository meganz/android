package mega.privacy.android.feature.documentscanner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.feature.documentscanner.data.smoother.ExponentialMovingAverageBoundarySmoother
import mega.privacy.android.feature.documentscanner.domain.smoother.BoundarySmoother

/**
 * Binding installed in [ViewModelComponent] so each ScanSessionViewModel gets
 * its own smoother instance. The impl carries mutable EMA state across calls;
 * scoping it any wider would leak history between scan sessions.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal abstract class BoundarySmootherModule {

    @Binds
    abstract fun bindBoundarySmoother(
        impl: ExponentialMovingAverageBoundarySmoother,
    ): BoundarySmoother
}
