package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.global.GlobalRequestListener
import mega.privacy.android.domain.repository.InitializationRepository
import javax.inject.Inject

internal class InitializationRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val globalRequestListener: GlobalRequestListener,
) : InitializationRepository {

    override suspend fun initializeGlobalRequestListener() {
        megaApiGateway.addRequestListener(globalRequestListener)
    }
}