package mega.privacy.android.app.utils

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.core.formatter.emoji.EmojiShortcodeConverter

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface EmojiShortcodeConverterEntryPoint {
    fun emojiShortcodeConverter(): EmojiShortcodeConverter
}
