package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.sortorder.SortOrderUseCases
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [SortOrderUseCases::class],
    components = [SingletonComponent::class]
)
object TestSortOrderUseCases {

    val getCloudSortOrder = mock<GetCloudSortOrder>()
    val getCameraSortOrder = mock<GetCameraSortOrder>()
    val getOthersSortOrder = mock<GetOthersSortOrder>()
    val getLinksSortOrder = mock<GetLinksSortOrder>()


    @Provides
    fun provideGetCloudSortOrder() = getCloudSortOrder

    @Provides
    fun provideGetCameraSortOrder() = getCameraSortOrder

    @Provides
    fun provideGetOthersSortOrder() = getOthersSortOrder

    @Provides
    fun provideGetLinksSortOrder() = getLinksSortOrder
}