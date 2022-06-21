package mega.privacy.android.app.di.settings

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.domain.repository.SettingsRepository
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
        fun provideAreChatLogsEnabled(settingsRepository: SettingsRepository): AreChatLogsEnabled =
            AreChatLogsEnabled(settingsRepository::isChatLoggingEnabled)

        @Provides
        fun provideAreSdkLogsEnabled(settingsRepository: SettingsRepository): AreSdkLogsEnabled =
            AreSdkLogsEnabled(settingsRepository::isSdkLoggingEnabled)

        @Provides
        fun provideSetSdkLogsEnabled(settingsRepository: SettingsRepository): SetSdkLogsEnabled =
            SetSdkLogsEnabled(settingsRepository::setSdkLoggingEnabled)

        @Provides
        fun provideSetChatLogsEnabled(settingsRepository: SettingsRepository): SetChatLogsEnabled =
            SetChatLogsEnabled(settingsRepository::setChatLoggingEnabled)
    }

}
