package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


/**
 * Clipboard module
 *
 */
@Module(includes = [InternalClipboardModule::class])
@InstallIn(SingletonComponent::class)
abstract class ClipboardModule