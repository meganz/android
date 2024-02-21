package mega.privacy.android.app.di.chat

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.actions.DeleteMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.ForwardMessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.MessageAction

@Module
@InstallIn(FragmentComponent::class)
internal class MessageActionModule {

    @Provides
    @IntoSet
    fun provideForwardActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> ForwardMessageAction(vm) }

    @Provides
    @IntoSet
    fun provideDeleteActionFactory(): (ChatViewModel) -> MessageAction =
        { vm -> DeleteMessageAction(vm) }
}