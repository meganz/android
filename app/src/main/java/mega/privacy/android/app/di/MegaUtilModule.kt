package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.utils.wrapper.DateUtilFacade
import mega.privacy.android.data.wrapper.DateUtilWrapper

@Module
@InstallIn(SingletonComponent::class)
abstract class MegaUtilModule {

    @Binds
    abstract fun bindDateUtilWrapper(utilFacade: DateUtilFacade): DateUtilWrapper
}
