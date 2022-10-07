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
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
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
    val setCloudSortOrder = mock<SetCloudSortOrder>()
    val setCameraSortOrder = mock<SetCameraSortOrder>()
    val setOthersSortOrder = mock<SetOthersSortOrder>()

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

    @Provides
    fun provideSetCloudSortOrder() = setCloudSortOrder

    @Provides
    fun provideSetCameraSortOrder() = setCameraSortOrder

    @Provides
    fun provideSetOthersSortOrder() = setOthersSortOrder
}