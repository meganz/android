package mega.privacy.android.app.fetcher

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Dimension
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.usecase.avatar.GetAvatarFileFromEmailUseCase
import mega.privacy.android.domain.usecase.avatar.GetAvatarFileFromHandleUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFirstNameUseCase
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import javax.inject.Inject

/**
 * Mega avatar fetcher
 *
 * @property context application context
 * @property data contact avatar
 * @property getAvatarFileFromHandleUseCase
 * @property getUserAvatarColorUseCase
 * @property getParticipantFirstNameUseCase
 */
class MegaAvatarFetcher(
    private val context: Context,
    private val data: ContactAvatar,
    private val options: Options,
    private val getAvatarFileFromHandleUseCase: GetAvatarFileFromHandleUseCase,
    private val getAvatarFileFromEmailUseCase: GetAvatarFileFromEmailUseCase,
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase,
    private val getParticipantFirstNameUseCase: GetParticipantFirstNameUseCase,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        runCatching {
            when (data) {
                is ContactAvatar.WithEmail -> getAvatarFileFromEmailUseCase(data.email, true)
                is ContactAvatar.WithId -> getAvatarFileFromHandleUseCase(data.id.id, true)
            }
        }
            .onFailure { Timber.e(it, "Error getting avatar file from handle or email") }
            .getOrNull()?.takeIf { it.length() > 0 }?.let { file ->
                return SourceFetchResult(
                    source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM),
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
                    dataSource = DataSource.DISK
                )
            }

        val avatarSize = maxOf(
            (options.size.width as Dimension.Pixels).px,
            (options.size.height as Dimension.Pixels).px
        )
        val fontSize = avatarSize / 2
        val drawable = TextDrawable.builder()
            .beginConfig()
            .width(avatarSize)
            .height(avatarSize)
            .fontSize(fontSize)
            .textColor(ContextCompat.getColor(context, R.color.white))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(
                AvatarUtil.getFirstLetter(getParticipantFirstNameUseCase(data.id.id).orEmpty()),
                getUserAvatarColorUseCase(data.id.id)
            )
        return ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    /**
     * Factory
     *
     * @property context
     * @property getAvatarFileFromHandleUseCase
     * @property getAvatarFileFromEmailUseCase
     * @property getUserAvatarColorUseCase
     * @property getParticipantFirstNameUseCase
     */
    class Factory @Inject constructor(
        @ApplicationContext private val context: Context,
        private val getAvatarFileFromHandleUseCase: GetAvatarFileFromHandleUseCase,
        private val getAvatarFileFromEmailUseCase: GetAvatarFileFromEmailUseCase,
        private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase,
        private val getParticipantFirstNameUseCase: GetParticipantFirstNameUseCase,
    ) : Fetcher.Factory<ContactAvatar> {

        override fun create(
            data: ContactAvatar,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return MegaAvatarFetcher(
                context = context,
                data = data,
                options = options,
                getAvatarFileFromHandleUseCase = getAvatarFileFromHandleUseCase,
                getUserAvatarColorUseCase = getUserAvatarColorUseCase,
                getParticipantFirstNameUseCase = getParticipantFirstNameUseCase,
                getAvatarFileFromEmailUseCase = getAvatarFileFromEmailUseCase,
            )
        }

        private fun isApplicable(data: ContactAvatar): Boolean {
            return when (data) {
                is ContactAvatar.WithEmail -> data.email.isNotEmpty()
                is ContactAvatar.WithId -> data.id.id != 0L
            }
        }
    }
}