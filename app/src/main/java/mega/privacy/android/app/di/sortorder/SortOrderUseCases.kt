package mega.privacy.android.app.di.sortorder

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.domain.repository.SortOrderRepository
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder

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
        GetCloudSortOrder(sortOrderRepository::getCloudSortOrder)

    /**
     * Provide the GetCameraSortOrder implementation
     */
    @Provides
    fun provideGetCameraSortOrder(sortOrderRepository: SortOrderRepository): GetCameraSortOrder =
        GetCameraSortOrder(sortOrderRepository::getCameraSortOrder)

    /**
     * Provide the GetOthersSortOrder implementation
     */
    @Provides
    fun provideGetOthersSortOrder(sortOrderRepository: SortOrderRepository): GetOthersSortOrder =
        GetOthersSortOrder(sortOrderRepository::getOthersSortOrder)

    /**
     * Provide the GetLinksSortOrder implementation
     */
    @Provides
    fun provideGetLinksSortOrder(sortOrderRepository: SortOrderRepository): GetLinksSortOrder =
        GetLinksSortOrder(sortOrderRepository::getLinksSortOrder)
}