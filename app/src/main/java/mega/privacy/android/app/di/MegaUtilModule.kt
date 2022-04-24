package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.jobservices.CameraUploadsServiceFacade
import mega.privacy.android.app.jobservices.CameraUploadsServiceWrapper
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilFacade
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilFacade
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.utils.JobUtilFacade
import mega.privacy.android.app.utils.permission.PermissionUtilFacade
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper

@Module
@InstallIn(SingletonComponent::class)
abstract class MegaUtilModule {

    @Binds
    abstract fun bindStringUtilWrapper(stringUtilFacade: StringUtilFacade): StringUtilWrapper

    @Binds
    abstract fun bindMegaUtilWrapper(utilFacade: MegaUtilFacade): MegaUtilWrapper

    @Binds
    abstract fun bindPermissionUtilsWrapper(permissionUtilFacade: PermissionUtilFacade): PermissionUtilWrapper

    @Binds
    abstract fun bindJobUtilWrapper(jobUtilFacade: JobUtilFacade): JobUtilWrapper

    @Binds
    abstract fun bindCameraUploadsServiceWrapper(cameraFacade: CameraUploadsServiceFacade): CameraUploadsServiceWrapper
}
