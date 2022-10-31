package mega.privacy.android.app.di.login

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.CompleteFastLogin
import mega.privacy.android.domain.usecase.DefaultCompleteFastLogin

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
}