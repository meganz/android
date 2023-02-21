package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.DefaultGetCurrentUserFullName
import mega.privacy.android.domain.usecase.DefaultGetExtendedAccountDetail
import mega.privacy.android.domain.usecase.DefaultGetUserFullName
import mega.privacy.android.domain.usecase.DefaultIsDatabaseEntryStale
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetFullAccountInfo
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.GetSpecificAccountDetail
import mega.privacy.android.domain.usecase.GetUserFullName
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import mega.privacy.android.domain.usecase.IsExtendedAccountDetailStale
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.impl.DefaultGetFullAccountInfo
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

    @Binds
    abstract fun bindGetFullAccountInfo(implementation: DefaultGetFullAccountInfo): GetFullAccountInfo

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

    @Binds
    abstract fun bindGetUserFullName(implementation: DefaultGetUserFullName): GetUserFullName

    companion object {

        @Provides
        fun provideGetSpecificAccountDetail(repository: AccountRepository) =
            GetSpecificAccountDetail(repository::getSpecificAccountDetail)

        @Provides
        fun provideGetPaymentMethod(repository: BillingRepository) =
            GetPaymentMethod(repository::getPaymentMethod)

        @Provides
        fun provideGetPricing(repository: BillingRepository) = GetPricing(repository::getPricing)

        @Provides
        fun provideGetNumberOfSubscription(repository: BillingRepository) =
            GetNumberOfSubscription(repository::getNumberOfSubscription)

        @Provides
        fun provideGetFileVersionsOption(repository: FileSystemRepository) =
            GetFileVersionsOption(repository::getFileVersionsOption)
    }
}