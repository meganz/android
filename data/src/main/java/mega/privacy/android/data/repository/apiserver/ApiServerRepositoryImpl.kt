package mega.privacy.android.data.repository.apiserver

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.apiserver.ApiServerMapper
import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import javax.inject.Inject

internal class ApiServerRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val apiServerMapper: ApiServerMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : ApiServerRepository {

    override suspend fun setPublicKeyPinning(enable: Boolean) = withContext(ioDispatcher) {
        megaApiGateway.setPublicKeyPinning(enable)
        megaApiFolderGateway.setPublicKeyPinning(enable)
    }

    override suspend fun changeApi(apiURL: String, disablePkp: Boolean) =
        withContext(ioDispatcher) {
            megaApiGateway.changeApiUrl(apiURL, disablePkp)
            megaApiFolderGateway.changeApiUrl(apiURL, disablePkp)
        }

    override suspend fun getCurrentApi() = withContext(ioDispatcher) {
        apiServerMapper(
            context.getSharedPreferences(API_SERVER_PREFERENCES, Context.MODE_PRIVATE)
                .getInt(API_SERVER, ApiServer.Production.value)
        )
    }

    override suspend fun setNewApi(apiServer: ApiServer) = withContext(ioDispatcher) {
        context.getSharedPreferences(API_SERVER_PREFERENCES, Context.MODE_PRIVATE)
            .edit().putInt(API_SERVER, apiServer.value).apply()
    }

    companion object {
        const val API_SERVER_PREFERENCES = "API_SERVER_PREFERENCES"
        const val API_SERVER = "API_SERVER"
    }
}