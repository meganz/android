package mega.privacy.android.app.di.login

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.BroadcastLogout
import mega.privacy.android.domain.usecase.CompleteFastLogin
import mega.privacy.android.domain.usecase.DefaultCompleteFastLogin
import mega.privacy.android.domain.usecase.InitialiseMegaChat
import mega.privacy.android.domain.usecase.LocalLogout
import mega.privacy.android.domain.usecase.MonitorLogout

/**
 * Login module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoginModule {

    /**
     * Provides [CompleteFastLogin] implementation.
     */
    @Binds
    abstract fun bindCompleteFastLogin(loginRepository: DefaultCompleteFastLogin): CompleteFastLogin

    companion object {

        /**
         * Provides [InitialiseMegaChat] implementation
         */
        @Provides
        fun bindInitialiseMegaChat(loginRepository: LoginRepository): InitialiseMegaChat =
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

        /**
         * Provides [LocalLogout]
         */
        @Provides
        fun provideLocalLogout(loginRepository: LoginRepository): LocalLogout =
            LocalLogout(loginRepository::localLogout)
    }
}