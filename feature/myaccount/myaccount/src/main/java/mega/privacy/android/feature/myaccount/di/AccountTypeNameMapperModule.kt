package mega.privacy.android.feature.myaccount.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.myaccount.presentation.mapper.AccountTypeNameMapper
import mega.privacy.android.shared.resources.SharedStringResourceProvider

/**
 * Provides [SharedStringResourceProvider] for account type name string resources.
 * Allows other modules (e.g. payment) to resolve account type name without depending on myaccount mapper directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountTypeNameMapperModule {

    @Binds
    abstract fun bindAccountTypeNameMapper(
        accountTypeNameMapper: AccountTypeNameMapper,
    ): SharedStringResourceProvider<AccountType?>
}
