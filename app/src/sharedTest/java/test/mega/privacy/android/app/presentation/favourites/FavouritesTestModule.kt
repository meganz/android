package test.mega.privacy.android.app.presentation.favourites

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.MegaUtilModule
import mega.privacy.android.app.di.homepage.favourites.FavouritesUseCases
import mega.privacy.android.app.di.homepage.favourites.OpenFileModule
import mega.privacy.android.app.domain.usecase.GetAllFavorites
import mega.privacy.android.app.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.app.globalmanagement.SortOrderManagementInterface
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FavouritesUseCases::class, MegaUtilModule::class, OpenFileModule::class]
)
object FavouritesTestModule {
    val getAllFavourites = mock<GetAllFavorites>()
    val getFavouriteFolderInfo = mock<GetFavouriteFolderInfo>()
    val megaUtilWrapper = mock<MegaUtilWrapper>()
    val stringUtilWrapper = mock<StringUtilWrapper>()

    @Provides
    fun provideGetAllFavorites(): GetAllFavorites = getAllFavourites

    @Provides
    fun provideStringUtilWrapper(): StringUtilWrapper = stringUtilWrapper

    @Provides
    fun provideGetFavouriteFolderInfo(): GetFavouriteFolderInfo = getFavouriteFolderInfo

    @Provides
    fun provideMegaUtilWrapper() : MegaUtilWrapper = megaUtilWrapper

    @Provides
    fun provideOpenFileWrapper() : OpenFileWrapper = mock()

    @Provides
    fun provideSortManagementInterface() : SortOrderManagementInterface = mock()
}