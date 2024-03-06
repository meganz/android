package mega.privacy.android.app.di.chat

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.actions.AvailableOfflineMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.ContactInfoMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.CopyMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.DeleteMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.EditLocationMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.EditMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.ForwardMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.ImportMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.InviteMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.MessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.OpenWithMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.SelectMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.SendMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.ShareMessageAction

@Module
@InstallIn(FragmentComponent::class)
internal class MessageActionModule {
    @Provides
    @IntoSet
    fun provideOpenWithMessageAction(): (ChatViewModel) -> MessageAction =
        { vm -> OpenWithMessageAction(vm) }

    @Provides
    @IntoSet
    fun provideForwardActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> ForwardMessageAction(vm) }


    @Provides
    @IntoSet
    fun provideInviteActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> InviteMessageAction(vm) }


    @Provides
    @IntoSet
    fun provideShareActionFactory(): (ChatViewModel) -> MessageAction =
        { _ -> ShareMessageAction() }

    @Provides
    @IntoSet
    fun provideSelectActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> SelectMessageAction(vm) }

    @Provides
    @IntoSet
    fun provideImportActionFactory(): (ChatViewModel) -> MessageAction =
        { _ -> ImportMessageAction() }

    @Provides
    @IntoSet
    fun provideEditActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> EditMessageAction(vm) }

    @Provides
    @IntoSet
    fun provideEditLocationActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> EditLocationMessageAction(vm) }

    @Provides
    @IntoSet
    fun provideCopyActionFactory(): (ChatViewModel) -> MessageAction =
        { _ -> CopyMessageAction() }

    @Provides
    @IntoSet
    fun provideContactInfoActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> ContactInfoMessageAction(vm) }


    @Provides
    @IntoSet
    fun provideSendMessageActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> SendMessageAction(vm) }

    @Provides
    @IntoSet
    fun provideAvailableOfflineActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> AvailableOfflineMessageAction(vm) }

    @Provides
    @IntoSet
    fun provideDeleteActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> DeleteMessageAction(vm) }
}