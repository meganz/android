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
import mega.privacy.android.domain.usecase.login.BroadcastFetchNodesFinish
import mega.privacy.android.domain.usecase.login.BroadcastLogout
import mega.privacy.android.domain.usecase.login.ChatLogout
import mega.privacy.android.domain.usecase.login.DefaultBackgroundFastLogin
import mega.privacy.android.domain.usecase.login.DefaultChatLogout
import mega.privacy.android.domain.usecase.login.DefaultFastLogin
import mega.privacy.android.domain.usecase.login.DefaultFetchNodes
import mega.privacy.android.domain.usecase.login.DefaultLocalLogout
import mega.privacy.android.domain.usecase.login.DefaultLogin
import mega.privacy.android.domain.usecase.login.DefaultLoginWith2FA
import mega.privacy.android.domain.usecase.login.FastLogin
import mega.privacy.android.domain.usecase.login.FetchNodes
import mega.privacy.android.domain.usecase.login.InitialiseMegaChat
import mega.privacy.android.domain.usecase.login.LocalLogout
import mega.privacy.android.domain.usecase.login.Login
import mega.privacy.android.domain.usecase.login.LoginWith2FA
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinish
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

    /**
     * Provides [LoginWith2FA]
     */
    @Binds
    abstract fun bindLoginWith2FA(useCase: DefaultLoginWith2FA): LoginWith2FA

    /**
     * Provides [FastLogin]
     */
    @Binds
    abstract fun bindFastLogin(useCase: DefaultFastLogin): FastLogin

    /**
     * Provides [FetchNodes]
     */
    @Binds
    abstract fun bindFetchNodes(useCase: DefaultFetchNodes): FetchNodes

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

        /**
         * Provides [MonitorFetchNodesFinish]
         */
        @Provides
        fun provideMonitorFetchNodesFinish(loginRepository: LoginRepository): MonitorFetchNodesFinish =
            MonitorFetchNodesFinish(loginRepository::monitorFetchNodesFinish)

        /**
         * Provides [BroadcastFetchNodesFinish]
         */
        @Provides
        fun provideBroadcastFetchNodesFinish(loginRepository: LoginRepository): BroadcastFetchNodesFinish =
            BroadcastFetchNodesFinish(loginRepository::broadcastFetchNodesFinish)
    }
}