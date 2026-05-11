package mega.privacy.android.feature.signin.external.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mega.privacy.android.domain.repository.security.GoogleSignInRepository
import mega.privacy.android.feature.signin.external.data.repository.security.DefaultGoogleSignInRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GoogleSignInModule {

    @Singleton
    @Binds
    internal abstract fun bindGoogleSignInRepository(
        impl: DefaultGoogleSignInRepository,
    ): GoogleSignInRepository
}
