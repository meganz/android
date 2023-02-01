package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.ClipboardRepository
import mega.privacy.android.domain.usecase.CopyToClipBoard

/**
 * Clipboard module
 *
 * Provides clipboard specific bindings
 *
 */
@Module
@DisableInstallInCheck
internal abstract class InternalClipboardModule {

    companion object {

        @Provides
        fun provideCopyToClipboard(clipboardRepository: ClipboardRepository): CopyToClipBoard =
            CopyToClipBoard(clipboardRepository::setClip)
    }
}