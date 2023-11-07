package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ApiServerRepository
import javax.inject.Inject

internal class ApiServerRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ApiServerRepository {

    override suspend fun setPublicKeyPinning(enable: Boolean) = withContext(ioDispatcher) {
        megaApiGateway.setPublicKeyPinning(enable)
        megaApiFolderGateway.setPublicKeyPinning(enable)
    }

    override suspend fun changeApiUrl(apiURL: String, disablePkp: Boolean) =
        withContext(ioDispatcher) {
            megaApiGateway.changeApiUrl(apiURL, disablePkp)
            megaApiFolderGateway.changeApiUrl(apiURL, disablePkp)
        }
}