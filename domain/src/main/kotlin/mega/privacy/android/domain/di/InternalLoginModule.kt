package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.BroadcastLogout
import mega.privacy.android.domain.usecase.CompleteFastLogin
import mega.privacy.android.domain.usecase.DefaultCompleteFastLogin
import mega.privacy.android.domain.usecase.InitialiseMegaChat
import mega.privacy.android.domain.usecase.LocalLogout
import mega.privacy.android.domain.usecase.MonitorLogout

@Module
@DisableInstallInCheck
internal abstract class InternalLoginModule {

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