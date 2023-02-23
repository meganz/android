package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.components.twemoji.EmojiManager
import mega.privacy.android.app.components.twemoji.wrapper.EmojiManagerWrapper

/**
 * DI module for Emoji wrappers
 */
@Module
@InstallIn(SingletonComponent::class)
class EmojiWrapperModule {

    companion object {

        /**
         * Provide [EmojiManagerWrapper]
         */
        @Provides
        fun provideEmojiWrapper(): EmojiManagerWrapper = object : EmojiManagerWrapper {
            override fun getFirstEmoji(text: String): Int? =
                EmojiManager.getInstance().getFirstEmoji(text)?.resource
        }
    }
}