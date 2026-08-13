package mega.privacy.android.core.formatter.emoji

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class EmojiShortcodeConverterModule {

    @Binds
    abstract fun bindEmojiShortcodeConverter(
        implementation: DefaultEmojiShortcodeConverter,
    ): EmojiShortcodeConverter
}
