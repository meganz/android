package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.chat.message.edit.GetContentFromMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.edit.GetContentFromNormalMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.edit.GetContentFromRichPreviewMessageUseCase

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GetMessageContentModule {

    @Binds
    @IntoSet
    abstract fun bindGetContentFromNormalMessageUseCase(creator: GetContentFromNormalMessageUseCase): GetContentFromMessageUseCase

    @Binds
    @IntoSet
    abstract fun bindGetContentFromRichPreviewMessageUseCase(creator: GetContentFromRichPreviewMessageUseCase): GetContentFromMessageUseCase
}