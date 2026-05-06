package mega.privacy.android.feature.signin.external.di

import android.content.Context
import androidx.credentials.CredentialManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mega.privacy.android.domain.repository.security.GoogleSignInRepository
import mega.privacy.android.feature.signin.external.data.gateway.GoogleSignInGateway
import mega.privacy.android.feature.signin.external.data.GoogleSignInGatewayImpl
import mega.privacy.android.feature.signin.external.data.repository.security.DefaultGoogleSignInRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GoogleSignInModule {

    @Singleton
    @Binds
    internal abstract fun bindGoogleSignInRepository(
        impl: DefaultGoogleSignInRepository,
    ): GoogleSignInRepository

    @Singleton
    @Binds
    internal abstract fun bindGoogleSignInGateway(
        impl: GoogleSignInGatewayImpl,
    ): GoogleSignInGateway

    companion object {
        @Provides
        @Singleton
        fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
            CredentialManager.create(context)
    }
}
