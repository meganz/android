package test.mega.privacy.android.app.di

import mega.privacy.android.domain.di.SharedUseCaseModule as DomainSharedUseCaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.SharedUseCaseModule
import mega.privacy.android.app.domain.usecase.CheckAccessErrorExtended
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.GetSpecificAccountDetail
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [SharedUseCaseModule::class, DomainSharedUseCaseModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestSharedUseCases {

    @Provides
    fun provideIsDatabaseEntryStale() = mock<IsDatabaseEntryStale>()

    @Provides
    fun provideCheckAccessErrorExtended() = mock<CheckAccessErrorExtended>()

    @Provides
    fun provideGetExtendedAccountDetail() = mock<GetExtendedAccountDetail>()

    @Provides
    fun provideGetSpecificAccountDetail() = mock<GetSpecificAccountDetail>()

    @Provides
    fun provideGetPaymentMethod() = mock<GetPaymentMethod>()

    @Provides
    fun provideGetPricing() = mock<GetPricing>()

    @Provides
    fun provideGetNumberOfSubscription() = mock<GetNumberOfSubscription>()
}
