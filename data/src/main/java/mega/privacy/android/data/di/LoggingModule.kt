package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.data.logging.LogFlowTree
import mega.privacy.android.data.qualifier.ChatLogger
import mega.privacy.android.data.qualifier.SdkLogger
import mega.privacy.android.data.repository.TimberLoggingRepository
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.usecase.CreateLogEntry
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoggingModule {
    @Singleton
    @Binds
    abstract fun bindLoggingRepository(repository: TimberLoggingRepository): LoggingRepository

    companion object {
        @SdkLogger
        @Provides
        fun provideSdkLogFlowTree(
            @SdkLogger useCase: CreateLogEntry,
            @IoDispatcher dispatcher: CoroutineDispatcher,
        ): LogFlowTree = LogFlowTree(dispatcher, useCase)

        @ChatLogger
        @Provides
        fun provideChatLogFlowTree(
            @ChatLogger useCase: CreateLogEntry,
            @IoDispatcher dispatcher: CoroutineDispatcher,
        ): LogFlowTree = LogFlowTree(dispatcher, useCase)
    }
}