package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.chat.message.delete.DeleteGeneralMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.delete.DeleteMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.delete.DeleteNodeAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.delete.DeletePendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.delete.DeleteVoiceClipMessageUseCase

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DeleteMessageModule {

    @Binds
    @IntoSet
    abstract fun bindDeleteGeneralMessageUseCase(creator: DeleteGeneralMessageUseCase): DeleteMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindDeleteNodeAttachmentMessageUseCase(creator: DeleteNodeAttachmentMessageUseCase): DeleteMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindDeleteVoiceClipMessageUseCase(creator: DeleteVoiceClipMessageUseCase): DeleteMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindDeletePendingMessageUseCase(creator: DeletePendingMessageUseCase): DeleteMessageUseCase
}

