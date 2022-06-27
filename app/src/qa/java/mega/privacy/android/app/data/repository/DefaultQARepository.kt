package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.data.gateway.DistributionGateway
import mega.privacy.android.app.domain.entity.Progress
import mega.privacy.android.app.domain.repository.QARepository
import javax.inject.Inject

/**
 * Default implementation of the [QARepository]
 *
 * @property distributionGateway
 */
class DefaultQARepository @Inject constructor(
    private val distributionGateway: DistributionGateway,
) : QARepository {

    override fun updateApp() = callbackFlow<Progress> {
        distributionGateway.autoUpdateIfAvailable()
            .addOnProgressListener {
                channel.trySend(Progress((it.apkBytesDownloaded / it.apkFileTotalBytes).toFloat()))
            }.addOnFailureListener {
                channel.close(it)
            }
    }
}