package mega.privacy.android.data.repository

import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.repository.DefaultAvatarRepository.Companion.AVATAR_PRIMARY_COLOR
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.BitmapFactoryWrapper
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Default [AvatarRepository] implementation
 *
 * @param megaApiGateway
 * @param cacheFolderGateway
 * @param sharingScope scope for share flow
 * @param ioDispatcher coroutine dispatcher to execute
 * @param bitmapFactoryWrapper
 */
internal class DefaultAvatarRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val cacheFolderGateway: CacheFolderGateway,
    private val avatarWrapper: AvatarWrapper,
    private val bitmapFactoryWrapper: BitmapFactoryWrapper,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AvatarRepository {

    private val monitorMyAvatarFile = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
        .mapNotNull { it.users?.find { user -> user.isOwnChange <= 0 && user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) && user.email == megaApiGateway.accountEmail } }
        .map { user ->
            deleteAvatarFile(user)
            loadAvatarFile(user)
        }
        .catch { Timber.e(it) }
        .flowOn(ioDispatcher)
        .shareIn(sharingScope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun monitorMyAvatarFile() = monitorMyAvatarFile

    private fun deleteAvatarFile(user: MegaUser) {
        val oldFile =
            cacheFolderGateway.buildAvatarFile(user.email + FileConstant.JPG_EXTENSION) ?: return
        if (oldFile.exists()) {
            oldFile.delete()
        }
    }

    private suspend fun loadAvatarFile(user: MegaUser): File? {
        val avatarFile =
            cacheFolderGateway.buildAvatarFile(user.email + FileConstant.JPG_EXTENSION)
                ?: return null
        megaApiGateway.getUserAvatar(user, avatarFile.absolutePath)
        return avatarFile
    }

    override suspend fun getMyAvatarColor(): Int {
        val user = megaApiGateway.getLoggedInUser() ?: return getColor(null)
        val avatarFile = getMyAvatarFile()
        if (avatarFile != null) {
            val avatarBitmap = bitmapFactoryWrapper.decodeFile(avatarFile.absolutePath,
                BitmapFactory.Options())
            if (avatarBitmap != null) {
                return avatarWrapper.getDominantColor(avatarBitmap)
            }
        }
        return getColor(megaApiGateway.getUserAvatarColor(user))
    }

    override suspend fun getMyAvatarFile(): File? =
        cacheFolderGateway.buildAvatarFile(megaApiGateway.accountEmail + FileConstant.JPG_EXTENSION)

    private fun getColor(color: String?): Int {
        return if (color == null) {
            avatarWrapper.getSpecificAvatarColor(AVATAR_PRIMARY_COLOR)
        } else Color.parseColor(color)
    }

    companion object {
        /**
         * refer [mega.privacy.android.app.utils.Constants.AVATAR_PRIMARY_COLOR]
         */
        private const val AVATAR_PRIMARY_COLOR = "AVATAR_PRIMARY_COLOR"
    }
}