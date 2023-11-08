package mega.privacy.android.domain.usecase.apiserver

import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import javax.inject.Inject

/**
 * Update api server use case
 *
 * @property apiServerRepository [ApiServerRepository]
 */
class UpdateApiServerUseCase @Inject constructor(
    private val apiServerRepository: ApiServerRepository,
) {

    /**
     * Invoke
     *
     * @param currentApi [ApiServer]
     * @param newApi [ApiServer]
     */
    suspend operator fun invoke(currentApi: ApiServer, newApi: ApiServer) {
        if (currentApi == newApi) return

        var disablePkp = false
        var setPkp: Boolean? = null

        when {
            currentApi == ApiServer.Sandbox3 || currentApi == ApiServer.Staging444 -> {
                setPkp = true
            }

            newApi == ApiServer.Sandbox3 || newApi == ApiServer.Staging444 -> {
                setPkp = false
                disablePkp = true
            }
        }

        with(apiServerRepository) {
            setPkp?.let { setPublicKeyPinning(it) }
            changeApi(newApi.url, disablePkp)
            setNewApi(newApi)
        }
    }
}