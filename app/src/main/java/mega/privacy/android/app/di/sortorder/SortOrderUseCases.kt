package mega.privacy.android.app.di.sortorder

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.GetCameraSortOrder
import mega.privacy.android.app.domain.usecase.GetCloudSortOrder

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
    fun provideGetCloudSortOrder(filesRepository: FilesRepository): GetCloudSortOrder =
        GetCloudSortOrder(filesRepository::getCloudSortOrder)

    /**
     * Provide the GetCameraSortOrder implementation
     */
    @Provides
    fun provideGetCameraSortOrder(filesRepository: FilesRepository): GetCameraSortOrder =
        GetCameraSortOrder(filesRepository::getCameraSortOrder)
}