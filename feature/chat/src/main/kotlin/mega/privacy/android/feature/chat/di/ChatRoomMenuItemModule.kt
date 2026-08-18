package mega.privacy.android.feature.chat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import mega.privacy.android.feature.chat.list.menu.ArchiveChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.ChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.ClearHistoryChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.LeaveChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.MuteChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.UnarchiveChatRoomMenuItem

/**
 * Contributes every chat room action into the [ChatRoomMenuItem] multibinding consumed by the
 * chat list view model, keyed by a unique [IntKey] whose ascending value fixes the action's
 * position in the sheet. Adding a new action means adding another binding here, with a key that
 * does not collide with an existing one — Dagger rejects duplicate keys at compile time — plus
 * its item. The mutually-exclusive archive/unarchive pair sits on adjacent keys; only one of the
 * pair ever passes `shouldDisplay`, while the single mute action toggles by chat state.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatRoomMenuItemModule {

    @Binds
    @IntoMap
    @IntKey(0)
    internal abstract fun bindClearHistory(item: ClearHistoryChatRoomMenuItem): ChatRoomMenuItem

    @Binds
    @IntoMap
    @IntKey(1)
    internal abstract fun bindMute(item: MuteChatRoomMenuItem): ChatRoomMenuItem

    @Binds
    @IntoMap
    @IntKey(2)
    internal abstract fun bindArchive(item: ArchiveChatRoomMenuItem): ChatRoomMenuItem

    @Binds
    @IntoMap
    @IntKey(3)
    internal abstract fun bindUnarchive(item: UnarchiveChatRoomMenuItem): ChatRoomMenuItem

    @Binds
    @IntoMap
    @IntKey(4)
    internal abstract fun bindLeave(item: LeaveChatRoomMenuItem): ChatRoomMenuItem
}
