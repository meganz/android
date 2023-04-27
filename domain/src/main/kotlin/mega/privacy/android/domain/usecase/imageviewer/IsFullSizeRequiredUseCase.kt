package mega.privacy.android.domain.usecase.imageviewer

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject


/**
 * The use case to check if full size required
 */
class IsFullSizeRequiredUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Is full size required
     *
     * @param node
     * @param fullSize
     *
     * @return Boolean
     */
    suspend operator fun invoke(
        node: TypedFileNode,
        fullSize: Boolean,
    ): Boolean = when {
        node.isTakenDown || node.type is VideoFileTypeInfo -> false
        node.size < SIZE_1_MB -> true
        node.size in SIZE_1_MB..SIZE_50_MB -> fullSize || settingsRepository.isMobileDataAllowed() || !(networkRepository.isMeteredConnection()
            ?: false)

        else -> false
    }

    companion object {
        private const val SIZE_1_MB = 1024 * 1024 * 1L
        private const val SIZE_50_MB = SIZE_1_MB * 50L
    }

}
