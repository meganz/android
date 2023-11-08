package mega.privacy.android.data.mapper.apiserver

import mega.privacy.android.domain.entity.apiserver.ApiServer
import javax.inject.Inject

/**
 * Api server mapper
 */
internal class ApiServerMapper @Inject constructor() {

    operator fun invoke(apiValue: Int) = when (apiValue) {
        0 -> ApiServer.Production
        1 -> ApiServer.Staging
        2 -> ApiServer.Staging444
        3 -> ApiServer.Sandbox3
        else -> ApiServer.Production
    }
}