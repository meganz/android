package mega.privacy.android.app.di.login

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.LoginRepository
import mega.privacy.android.app.domain.usecase.*

/**
 * Login module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoginModule {

    companion object {
        @Provides
        fun provideFastLogin(loginRepository: LoginRepository): FastLogin =
            FastLogin(loginRepository::fastLogin)

        @Provides
        fun provideFetchNodes(loginRepository: LoginRepository): FetchNodes =
            FetchNodes(loginRepository::fetchNodes)

        @Provides
        fun provideInitMegaChat(loginRepository: LoginRepository): InitMegaChat =
            InitMegaChat(loginRepository::initMegaChat)
    }
}