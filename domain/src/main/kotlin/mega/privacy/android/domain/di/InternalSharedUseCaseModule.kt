package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.DefaultGetExtendedAccountDetail
import mega.privacy.android.domain.usecase.DefaultIsDatabaseEntryStale
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetSpecificAccountDetail
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale

/**
 * Shared use case module
 *
 * Provides use case is shared with other components
 */
@Module
@DisableInstallInCheck
internal abstract class InternalSharedUseCaseModule {

    /**
     * Bind is database entry stale
     *
     */
    @Binds
    abstract fun bindIsDatabaseEntryStale(implementation: DefaultIsDatabaseEntryStale): IsDatabaseEntryStale

    /**
     * Bind get extended account detail
     *
     */
    @Binds
    abstract fun bindGetExtendedAccountDetail(implementation: DefaultGetExtendedAccountDetail): GetExtendedAccountDetail

    companion object {

        @Provides
        fun provideGetSpecificAccountDetail(repository: AccountRepository) =
            GetSpecificAccountDetail(repository::getSpecificAccountDetail)

        @Provides
        fun provideGetPaymentMethod(repository: BillingRepository) =
            GetPaymentMethod(repository::getPaymentMethod)
    }
}