package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.billing.GetActiveSubscription
import mega.privacy.android.domain.usecase.billing.IsBillingAvailable
import mega.privacy.android.domain.usecase.billing.MonitorBillingEvent
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.domain.usecase.billing.QuerySkus

/**
 * Provide all Billing UseCases
 *
 */
@Module
@DisableInstallInCheck
internal abstract class InternalBillingModule {
    companion object {
        @Provides
        fun provideQuerySkus(repository: BillingRepository) = QuerySkus(repository::querySkus)

        @Provides
        fun provideQueryPurchase(repository: BillingRepository) =
            QueryPurchase(repository::queryPurchase)

        @Provides
        fun provideMonitorBillingEvent(repository: BillingRepository) =
            MonitorBillingEvent(repository::monitorBillingEvent)

        @Provides
        fun provideIsBillingAvailable(repository: BillingRepository) =
            IsBillingAvailable(repository::isBillingAvailable)

        @Provides
        fun provideGetActiveSubscription(repository: BillingRepository) =
            GetActiveSubscription(repository::getActiveSubscription)
    }
}