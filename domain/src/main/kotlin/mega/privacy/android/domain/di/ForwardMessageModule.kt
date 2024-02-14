package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardContactUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardGiphyUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardLocationUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardNodeAttachmentUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardNormalMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardRichPreviewUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardVoiceClipUseCase

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ForwardMessageModule {

    @Binds
    @IntoSet
    abstract fun bindForwardNormalMessageUseCase(creator: ForwardNormalMessageUseCase): ForwardMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindForwardContactUseCase(creator: ForwardContactUseCase): ForwardMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindForwardNodeAttachmentUseCase(creator: ForwardNodeAttachmentUseCase): ForwardMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindForwardVoiceClipUseCase(creator: ForwardVoiceClipUseCase): ForwardMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindForwardRichPreviewUseCase(creator: ForwardRichPreviewUseCase): ForwardMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindForwardLocationUseCase(creator: ForwardLocationUseCase): ForwardMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindForwardGiphyUseCase(creator: ForwardGiphyUseCase): ForwardMessageUseCase
}

