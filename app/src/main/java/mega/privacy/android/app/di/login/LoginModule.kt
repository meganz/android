package mega.privacy.android.app.di.login

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.*

/**
 * Login module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoginModule {

    @Binds
    abstract fun bindFastLogin(useCase: DefaultFastLogin): FastLogin

    @Binds
    abstract fun bindFetchNodes(useCase: DefaultFetchNodes): FetchNodes

    @Binds
    abstract fun bindInitMegaChat(useCase: DefaultInitMegaChat): InitMegaChat
}