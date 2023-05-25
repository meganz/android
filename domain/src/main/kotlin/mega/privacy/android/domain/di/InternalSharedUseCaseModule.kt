package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.DefaultGetCurrentUserFullName
import mega.privacy.android.domain.usecase.DefaultGetExtendedAccountDetail
import mega.privacy.android.domain.usecase.DefaultIsDatabaseEntryStale
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import mega.privacy.android.domain.usecase.IsExtendedAccountDetailStale
import mega.privacy.android.domain.usecase.impl.DefaultIsExtendedAccountDetailStale

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

    @Binds
    abstract fun bindIsExtendedAccountDetailStale(implementation: DefaultIsExtendedAccountDetailStale): IsExtendedAccountDetailStale

    /**
     * Bind get extended account detail
     *
     */
    @Binds
    abstract fun bindGetExtendedAccountDetail(implementation: DefaultGetExtendedAccountDetail)
            : GetExtendedAccountDetail

    @Binds
    abstract fun bindGetCurrentUserFullName(implementation: DefaultGetCurrentUserFullName)
            : GetCurrentUserFullName


    companion object {

        @Provides
        fun provideGetPaymentMethod(repository: BillingRepository) =
            GetPaymentMethod(repository::getPaymentMethod)

        @Provides
        fun provideGetPricing(repository: BillingRepository) = GetPricing(repository::getPricing)

        @Provides
        fun provideGetNumberOfSubscription(repository: BillingRepository) =
            GetNumberOfSubscription(repository::getNumberOfSubscription)
    }
}