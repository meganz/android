package mega.privacy.android.data.repository.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.files.FingerprintRepository
import javax.inject.Inject

class FingerprintRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) : FingerprintRepository {
    override suspend fun getFingerprint(filePath: String) = withContext(ioDispatcher) {
        megaApiGateway.getFingerprint(filePath)
    }
}