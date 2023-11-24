package mega.privacy.android.domain.di

import dagger.MapKey
import mega.privacy.android.domain.entity.chat.ChatMessageType

@MapKey
internal annotation class ChatMessageTypeKey(val value: ChatMessageType)