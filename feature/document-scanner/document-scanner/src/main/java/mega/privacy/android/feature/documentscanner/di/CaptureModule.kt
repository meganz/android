package mega.privacy.android.feature.documentscanner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.documentscanner.data.capture.DefaultDocumentPageCapturer
import mega.privacy.android.feature.documentscanner.domain.capture.DocumentPageCapturer

/**
 * Hilt bindings for the capture pipeline (frame → warp → store → page).
 *
 * The pipeline is stateless (holds only injected collaborators), so the binding
 * is intentionally unscoped — a fresh instance per injection, no `@Singleton`.
 * Installed in [SingletonComponent] to match the module's other bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {

    @Binds
    internal abstract fun bindDocumentPageCapturer(
        impl: DefaultDocumentPageCapturer,
    ): DocumentPageCapturer
}
