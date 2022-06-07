package mega.privacy.android.app.di.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.app.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.DefaultAreChatLogsEnabled
import mega.privacy.android.app.domain.usecase.DefaultAreSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.DefaultSetChatLogsEnabled
import mega.privacy.android.app.domain.usecase.DefaultSetSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.app.domain.usecase.SetSdkLogsEnabled

/**
 * Settings module
 *
 * Provides dependencies used by multiple screens in the settings package
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindSetChatLogsEnabled(useCase: DefaultSetChatLogsEnabled): SetChatLogsEnabled

    @Binds
    abstract fun bindSetSdkLogsEnabled(useCase: DefaultSetSdkLogsEnabled): SetSdkLogsEnabled

    @Binds
    abstract fun bindAreChatLogsEnabled(useCase: DefaultAreChatLogsEnabled): AreChatLogsEnabled

    @Binds
    abstract fun bindAreSdkLogsEnabled(useCase: DefaultAreSdkLogsEnabled): AreSdkLogsEnabled

}
