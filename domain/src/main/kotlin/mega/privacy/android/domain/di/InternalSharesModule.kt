package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.usecase.shares.DefaultGetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetContactItemFromInShareFolder

@Module
@DisableInstallInCheck
abstract class InternalSharesModule {

    /**
     * Provides [GetContactItemFromInShareFolder] implementation.
     */
    @Binds
    abstract fun bindGetContactItemFromInShareFolder(useCase: DefaultGetContactItemFromInShareFolder): GetContactItemFromInShareFolder
}