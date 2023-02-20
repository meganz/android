package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.BroadcastLogout
import mega.privacy.android.domain.usecase.BackgroundFastLogin
import mega.privacy.android.domain.usecase.DefaultBackgroundFastLogin
import mega.privacy.android.domain.usecase.InitialiseMegaChat
import mega.privacy.android.domain.usecase.LocalLogout
import mega.privacy.android.domain.usecase.MonitorLogout
import javax.inject.Singleton

@Module
@DisableInstallInCheck
internal abstract class InternalLoginModule {

    /**
     * Provides [BackgroundFastLogin] implementation.
     */
    @Binds
    abstract fun bindCompleteFastLogin(loginRepository: DefaultBackgroundFastLogin): BackgroundFastLogin

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

        /**
         * Provides [LocalLogout]
         */
        @Provides
        fun provideLocalLogout(loginRepository: LoginRepository): LocalLogout =
            LocalLogout(loginRepository::localLogout)

        @LoginMutex
        @Singleton
        @Provides
        fun provideLoginMutex(): Mutex = Mutex()
    }
}