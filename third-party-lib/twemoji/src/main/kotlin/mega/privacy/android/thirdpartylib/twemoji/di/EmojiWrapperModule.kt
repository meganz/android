package mega.privacy.android.thirdpartylib.twemoji.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.thirdpartylib.twemoji.wrapper.EmojiManagerWrapper
import mega.privacy.android.thirdpartylib.twemoji.wrapper.EmojiManagerWrapperImpl

/**
 * DI module for Emoji wrappers
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EmojiWrapperModule {

    /**
     * Provide instance of EmojiManagerWrapper
     */
    @Binds
    abstract fun bindEmojiManagerWrapper(implementation: EmojiManagerWrapperImpl): EmojiManagerWrapper
}