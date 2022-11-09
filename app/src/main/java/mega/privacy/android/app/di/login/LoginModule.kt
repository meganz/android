package mega.privacy.android.app.di.login

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.CompleteFastLogin
import mega.privacy.android.domain.usecase.DefaultCompleteFastLogin
import mega.privacy.android.domain.usecase.InitialiseMegaChat

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
    }


}