package mega.privacy.android.app.di.settings

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.domain.repository.SupportRepository
import mega.privacy.android.app.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.app.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.GetSupportEmail
import mega.privacy.android.app.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.app.domain.usecase.SetSdkLogsEnabled
import mega.privacy.android.app.presentation.settings.model.PreferenceResource

/**
 * Settings module
 *
 * Provides dependencies used by multiple screens in the settings package
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    companion object {
        @Provides
        fun provideGetSupportEmail(supportRepository: SupportRepository): GetSupportEmail =
            GetSupportEmail(supportRepository::getSupportEmail)

        @Provides
        @ElementsIntoSet
        fun providePreferenceResourceSet(): Set<@JvmSuppressWildcards PreferenceResource> = setOf()

        @Provides
        fun provideAreChatLogsEnabled(repository: LoggingRepository): AreChatLogsEnabled =
            AreChatLogsEnabled(repository::isChatLoggingEnabled)

        @Provides
        fun provideAreSdkLogsEnabled(repository: LoggingRepository): AreSdkLogsEnabled =
            AreSdkLogsEnabled(repository::isSdkLoggingEnabled)

        @Provides
        fun provideSetSdkLogsEnabled(repository: LoggingRepository): SetSdkLogsEnabled =
            SetSdkLogsEnabled(repository::setSdkLoggingEnabled)

        @Provides
        fun provideSetChatLogsEnabled(repository: LoggingRepository): SetChatLogsEnabled =
            SetChatLogsEnabled(repository::setChatLoggingEnabled)
    }

}
