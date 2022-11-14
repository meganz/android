package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.node.FolderNode
import javax.inject.Inject

/**
 * Default get device type
 */
class DefaultGetDeviceType @Inject constructor() : GetDeviceType {
    override suspend fun invoke(folder: FolderNode): DeviceType {
        return when {
            winRegex.containsMatchIn(folder.name) -> DeviceType.Windows
            linuxRegex.containsMatchIn(folder.name) -> DeviceType.Linux
            macRegex.containsMatchIn(folder.name) -> DeviceType.Mac
            extRegex.containsMatchIn(folder.name) -> DeviceType.ExternalDrive
            else -> DeviceType.Unknown
        }
    }

    private val winRegex = Regex("win|desktop", RegexOption.IGNORE_CASE)
    private val linuxRegex = Regex("linux|debian|ubuntu|centos", RegexOption.IGNORE_CASE)
    private val macRegex = Regex("mac", RegexOption.IGNORE_CASE)
    private val extRegex = Regex("ext|drive", RegexOption.IGNORE_CASE)
}