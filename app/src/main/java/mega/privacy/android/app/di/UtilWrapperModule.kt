package mega.privacy.android.app.di

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.DefaultGetNodeLocationInfo
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.notifications.CameraUploadsNotificationManager
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.permission.PermissionUtilWrapperImpl
import mega.privacy.android.app.utils.wrapper.CameraEnumeratorWrapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilFacade
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.app.utils.wrapper.SetLogoutFlagWrapperImpl
import mega.privacy.android.app.utils.wrapper.SetupMegaChatApiWrapperImpl
import mega.privacy.android.data.facade.security.SetLogoutFlagWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.global.SetupMegaChatApiWrapper
import mega.privacy.android.data.wrapper.ApplicationIpAddressWrapper
import mega.privacy.android.data.wrapper.ApplicationWrapper
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.shared.resources.R as sharedR
import org.webrtc.Camera1Enumerator
import org.webrtc.CameraEnumerator

/**
 * Util wrapper module
 *
 * Module for providing temporary wrappers around static util methods. All of these dependencies
 * need to be removed during the refactoring process.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilWrapperModule {

    /**
     * Bind mega node util wrapper
     */
    @Binds
    abstract fun bindMegaNodeUtilWrapper(implementation: MegaNodeUtilFacade): MegaNodeUtilWrapper

    @Binds
    abstract fun bindPermissionUtilWrapper(implementation: PermissionUtilWrapperImpl): PermissionUtilWrapper

    @Binds
    abstract fun bindGetNodeLocation(implementation: DefaultGetNodeLocationInfo): GetNodeLocationInfo

    @Binds
    abstract fun bindSetLogoutFlagWrapper(implementation: SetLogoutFlagWrapperImpl): SetLogoutFlagWrapper

    @Binds
    abstract fun bindSetupMegaChatApiWrapper(implementation: SetupMegaChatApiWrapperImpl): SetupMegaChatApiWrapper


    companion object {

        @Provides
        fun provideFetchNodeWrapper(megaApiGateway: MegaApiGateway): FetchNodeWrapper =
            FetchNodeWrapper(megaApiGateway::getMegaNodeByHandle)

        /**
         * provide Avatar Wrapper
         */
        @Provides
        fun provideAvatarWrapper() = object : AvatarWrapper {
            override fun getDominantColor(bimap: Bitmap): Int = AvatarUtil.getDominantColor(bimap)

            override fun getSpecificAvatarColor(typeColor: String): Int =
                AvatarUtil.getSpecificAvatarColor(typeColor)

            override fun getFirstLetter(name: String): String =
                AvatarUtil.getFirstLetter(name)
        }


        /**
         * Provides the [StringWrapper]
         */
        @Provides
        fun provideStringWrapper(@ApplicationContext context: Context) =
            object : StringWrapper {
                override fun getProgressSize(progress: Long, size: Long): String =
                    Util.getProgressSize(context, progress, size)

                override fun encodeBase64(string: String, flags: Int): String =
                    Base64.encodeToString(string.toByteArray(Charsets.UTF_8), flags)

                override fun decodeBase64(base64: String, flags: Int): String =
                    try {
                        Base64.decode(base64.trim(), flags).toString(Charsets.UTF_8)
                    } catch (ignore: IllegalArgumentException) {
                        Base64.decode(base64.trim(), Base64.URL_SAFE).toString(Charsets.UTF_8)
                    }

                override fun getSizeString(size: Long) = Util.getSizeString(size, context)

                override fun getErrorStringResource(megaException: MegaException) =
                    context.getString(megaException.getErrorStringId())

                override fun getCloudDriveSection() =
                    context.getString(R.string.section_cloud_drive)

                override fun getRubbishBinSection() =
                    context.getString(sharedR.string.general_section_rubbish_bin)

                override fun getTitleIncomingSharesExplorer() =
                    context.getString(R.string.title_incoming_shares_explorer)

                override fun getErrorStorageQuota() =
                    context.getString(R.string.error_share_owner_storage_quota)

                override fun getSavedForOfflineNew() =
                    context.getString(R.string.section_saved_for_offline_new)
            }

        /**
         * provide default implementation for FileUtilWrapper
         */
        @Provides
        fun providesFileUtilWrapper() = object : FileUtilWrapper {}

        /**
         * Provides the [ApplicationIpAddressWrapper]
         */
        @Provides
        fun provideApplicationIpAddressWrapper(application: Application) =
            object : ApplicationIpAddressWrapper {
                override fun setIpAddress(ipAddress: String?) {
                    (application as MegaApplication).localIpAddress = ipAddress
                }

                override fun getIpAddress(): String? {
                    return (application as MegaApplication).localIpAddress
                }
            }

        /**
         * Provides the [CameraUploadsNotificationManagerWrapper]
         */
        @Provides
        fun provideNotificationHelper(cameraUploadsNotificationManager: CameraUploadsNotificationManager) =
            object : CameraUploadsNotificationManagerWrapper {
                override fun getForegroundInfo() =
                    cameraUploadsNotificationManager.getForegroundInfo()
            }

        /**
         * Provides the [ApplicationWrapper]
         */
        @Provides
        fun provideApplicationWrapper() =
            object : ApplicationWrapper {
                override fun setHeartBeatAlive(isAlive: Boolean) {
                    MegaApplication.setHeartBeatAlive(isAlive)
                }
            }

        /**
         * Provides the [CookieEnabledCheckWrapper]
         */
        @Provides
        fun provideCookieEnabledCheckWrapper(): CookieEnabledCheckWrapper =
            object : CookieEnabledCheckWrapper {
                override fun checkEnabledCookies() {
                    MegaApplication.getInstance().checkEnabledCookies()
                }
            }

        /**
         * Provides [CameraEnumeratorWrapper]
         */
        @Provides
        fun provideCameraEnumeratorWrapper(): CameraEnumeratorWrapper =
            object : CameraEnumeratorWrapper {
                override fun invoke(): CameraEnumerator =
                    Camera1Enumerator(true)
            }
    }
}
