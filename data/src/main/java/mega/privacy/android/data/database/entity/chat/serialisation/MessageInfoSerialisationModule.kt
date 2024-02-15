package mega.privacy.android.data.database.entity.chat.serialisation

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.RichPreviewEntity
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatRichPreviewInfo
import mega.privacy.android.domain.entity.node.FileNode

/**
 * MessageInfoSerialisationModule
 */
val messageInfoSerialisationModule = SerializersModule {
    polymorphic(ChatGifInfo::class) {
        subclass(GiphyEntity::class)
    }
    polymorphic(ChatGeolocationInfo::class) {
        subclass(ChatGeolocationEntity::class)
    }
    polymorphic(ChatRichPreviewInfo::class) {
        subclass(RichPreviewEntity::class)
    }
    polymorphic(FileNode::class) {
        subclass(ChatNodeEntity::class)
    }
}