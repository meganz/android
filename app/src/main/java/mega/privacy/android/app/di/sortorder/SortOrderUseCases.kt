package mega.privacy.android.app.di.sortorder

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import mega.privacy.android.domain.usecase.DefaultGetCameraSortOrder
import mega.privacy.android.domain.usecase.DefaultGetCloudSortOrder
import mega.privacy.android.domain.usecase.DefaultGetLinksSortOrder
import mega.privacy.android.domain.usecase.DefaultGetOfflineSortOrder
import mega.privacy.android.domain.usecase.DefaultGetOthersSortOrder
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetOfflineSortOrder
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder

/**
 * Provides the use case implementation regarding sort order
 */
@Module
@InstallIn(SingletonComponent::class)
class SortOrderUseCases {

    /**
     * Provide the GetCloudSortOrder implementation
     */
    @Provides
    fun provideGetCloudSortOrder(sortOrderRepository: SortOrderRepository): GetCloudSortOrder =
        DefaultGetCloudSortOrder(sortOrderRepository)

    /**
     * Provide the GetCameraSortOrder implementation
     */
    @Provides
    fun provideGetCameraSortOrder(sortOrderRepository: SortOrderRepository): GetCameraSortOrder =
        DefaultGetCameraSortOrder(sortOrderRepository)

    /**
     * Provide the GetOthersSortOrder implementation
     */
    @Provides
    fun provideGetOthersSortOrder(sortOrderRepository: SortOrderRepository): GetOthersSortOrder =
        DefaultGetOthersSortOrder(sortOrderRepository)

    /**
     * Provide the GetLinksSortOrder implementation
     */
    @Provides
    fun provideGetLinksSortOrder(sortOrderRepository: SortOrderRepository): GetLinksSortOrder =
        DefaultGetLinksSortOrder(sortOrderRepository)

    /**
     * Provide the GetOfflineSortOrder implementation
     */
    @Provides
    fun provideGetOfflineSortOrder(sortOrderRepository: SortOrderRepository): GetOfflineSortOrder =
        DefaultGetOfflineSortOrder(sortOrderRepository)

    /**
     * Provide the SetOfflineSortOrder implementation
     */
    @Provides
    fun provideSetOfflineSortOrder(sortOrderRepository: SortOrderRepository): SetOfflineSortOrder =
        SetOfflineSortOrder(sortOrderRepository::setOfflineSortOrder)

    /**
     * Provide the SetCloudSortOrder implementation
     */
    @Provides
    fun provideSetCloudSortOrder(sortOrderRepository: SortOrderRepository): SetCloudSortOrder =
        SetCloudSortOrder(sortOrderRepository::setCloudSortOrder)

    /**
     * Provide the SetCameraSortOrder implementation
     */
    @Provides
    fun provideSetCameraSortOrder(sortOrderRepository: SortOrderRepository): SetCameraSortOrder =
        SetCameraSortOrder(sortOrderRepository::setCameraSortOrder)

    /**
     * Provide the SetOthersSortOrder implementation
     */
    @Provides
    fun provideSetOthersSortOrder(sortOrderRepository: SortOrderRepository): SetOthersSortOrder =
        SetOthersSortOrder(sortOrderRepository::setOthersSortOrder)
}