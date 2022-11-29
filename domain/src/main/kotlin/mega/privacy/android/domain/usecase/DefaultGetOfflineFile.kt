package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.offline.InboxOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.FileRepository
import java.io.File
import javax.inject.Inject

class DefaultGetOfflineFile @Inject constructor(private val fileRepository: FileRepository) : GetOfflineFile {
    override suspend fun invoke(offlineInformation: OfflineNodeInformation): File {
        return when (offlineInformation) {
            is InboxOfflineNodeInformation -> getFile(
                fileRepository.getOfflineInboxPath(),
                offlineInformation.path,
                offlineInformation.name)
            is IncomingShareOfflineNodeInformation -> getFile(
                fileRepository.getOfflinePath(),
                offlineInformation.incomingHandle,
                offlineInformation.path,
                offlineInformation.name)
            is OtherOfflineNodeInformation -> getFile(
                fileRepository.getOfflinePath(),
                offlineInformation.path,
                offlineInformation.name)
        }
    }

    private fun getFile(vararg paths: String) =
        File(paths.filterNot { it == File.separator }
            .joinToString(separator = File.separator))

}