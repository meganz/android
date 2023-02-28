package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.BroadcastFinishActivity
import mega.privacy.android.domain.usecase.Logout
import mega.privacy.android.domain.usecase.MonitorFinishActivity
import mega.privacy.android.domain.usecase.login.BackgroundFastLogin
import mega.privacy.android.domain.usecase.login.BroadcastLogout
import mega.privacy.android.domain.usecase.login.ChatLogout
import mega.privacy.android.domain.usecase.login.DefaultBackgroundFastLogin
import mega.privacy.android.domain.usecase.login.DefaultChatLogout
import mega.privacy.android.domain.usecase.login.DefaultLocalLogout
import mega.privacy.android.domain.usecase.login.DefaultLogin
import mega.privacy.android.domain.usecase.login.InitialiseMegaChat
import mega.privacy.android.domain.usecase.login.LocalLogout
import mega.privacy.android.domain.usecase.login.Login
import mega.privacy.android.domain.usecase.login.MonitorLogout
import javax.inject.Singleton

@Module
@DisableInstallInCheck
internal abstract class InternalLoginModule {

    /**
     * Provides [BackgroundFastLogin] implementation.
     */
    @Binds
    abstract fun bindCompleteFastLogin(useCase: DefaultBackgroundFastLogin): BackgroundFastLogin

    /**
     * Provides [LocalLogout]
     */
    @Binds
    abstract fun bindLocalLogout(useCase: DefaultLocalLogout): LocalLogout

    /**
     * Provides [ChatLogout]
     */
    @Binds
    abstract fun bindChatLogout(useCase: DefaultChatLogout): ChatLogout

    /**
     * Provides [Login]
     */
    @Binds
    abstract fun bindLogin(useCase: DefaultLogin): Login

    companion object {

        /**
         * Provides [InitialiseMegaChat] implementation
         */
        @Provides
        fun provideInitialiseMegaChat(loginRepository: LoginRepository): InitialiseMegaChat =
            InitialiseMegaChat(loginRepository::initMegaChat)

        /**
         * Provides [MonitorLogout]
         */
        @Provides
        fun provideMonitorLogout(loginRepository: LoginRepository): MonitorLogout =
            MonitorLogout(loginRepository::monitorLogout)

        /**
         * Provides [BroadcastLogout]
         */
        @Provides
        fun provideBroadcastLogout(loginRepository: LoginRepository): BroadcastLogout =
            BroadcastLogout(loginRepository::broadcastLogout)

        @LoginMutex
        @Singleton
        @Provides
        fun provideLoginMutex(): Mutex = Mutex()


        /**
         * Provides the Use Case [Logout]
         */
        @Provides
        fun provideLogout(repository: LoginRepository): Logout = Logout(repository::logout)

        /**
         * Provides [MonitorFinishActivity]
         */
        @Provides
        fun provideMonitorFinishActivity(loginRepository: LoginRepository): MonitorFinishActivity =
            MonitorFinishActivity(loginRepository::monitorFinishActivity)

        /**
         * Provides [BroadcastFinishActivity]
         */
        @Provides
        fun provideBroadcastFinishActivity(loginRepository: LoginRepository): BroadcastFinishActivity =
            BroadcastFinishActivity(loginRepository::broadcastFinishActivity)
    }
}