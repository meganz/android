package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.sortorder.SortOrderUseCases
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetOfflineSortOrder
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
    val getOfflineSortOrder = mock<GetOfflineSortOrder>()

    val setOfflineSortOrder = mock<SetOfflineSortOrder>()

    @Provides
    fun provideGetCloudSortOrder() = getCloudSortOrder

    @Provides
    fun provideGetCameraSortOrder() = getCameraSortOrder

    @Provides
    fun provideGetOthersSortOrder() = getOthersSortOrder

    @Provides
    fun provideGetLinksSortOrder() = getLinksSortOrder

    @Provides
    fun provideGetOfflineSortOrder() = getOfflineSortOrder

    @Provides
    fun provideSetOfflineSortOrder() = setOfflineSortOrder
}