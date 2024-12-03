package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.BusinessRepository
import mega.privacy.android.domain.usecase.DefaultIsUserLoggedIn
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.IsUserLoggedIn

/**
 * Account module.
 *
 * Provides all account implementations.
 *
 */
@Module
@DisableInstallInCheck
internal abstract class InternalAccountModule {
    /**
     * Binds the Use Case [IsUserLoggedIn] to its implementation [DefaultIsUserLoggedIn]
     */
    @Binds
    abstract fun bindIsUserLoggedIn(useCase: DefaultIsUserLoggedIn): IsUserLoggedIn

    companion object {

        /**
         * Provides the Use Case [IsBusinessAccountActive]
         */
        @Provides
        fun provideIsBusinessAccountActive(repository: BusinessRepository): IsBusinessAccountActive =
            IsBusinessAccountActive(repository::isBusinessAccountActive)
    }
}
