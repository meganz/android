package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.favourites.facade.DateUtilFacade
import mega.privacy.android.app.presentation.favourites.facade.DateUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilFacade
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilFacade
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper

@Module
@InstallIn(SingletonComponent::class)
abstract class MegaUtilModule {

    @Binds
    abstract fun bindStringUtilWrapper(stringUtilFacade: StringUtilFacade): StringUtilWrapper

    @Binds
    abstract fun bindMegaUtilWrapper(utilFacade: MegaUtilFacade): MegaUtilWrapper

    @Binds
    abstract fun bindDateUtilWrapper(utilFacade: DateUtilFacade): DateUtilWrapper
}
