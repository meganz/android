package mega.privacy.android.domain.usecase.apiserver

import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import javax.inject.Inject

/**
 * Update api server use case
 *
 * @property apiServerRepository [ApiServerRepository]
 * @property getCurrentApiServerUseCase [GetCurrentApiServerUseCase]
 */
class UpdateApiServerUseCase @Inject constructor(
    private val apiServerRepository: ApiServerRepository,
    private val getCurrentApiServerUseCase: GetCurrentApiServerUseCase,
) {

    /**
     * Invoke
     *
     * @param currentApi [ApiServer], null if want to set the server when MegaApplication is created.
     * @param newApi [ApiServer], null if want to set the server when MegaApplication is created.
     */
    suspend operator fun invoke(currentApi: ApiServer? = null, newApi: ApiServer? = null) {
        val storedApi = currentApi ?: getCurrentApiServerUseCase()

        if (storedApi == newApi || (newApi == null && storedApi == ApiServer.Production)) return

        var disablePkp = false
        var setPkp: Boolean? = null

        when {
            newApi != null && (storedApi == ApiServer.Sandbox3 || storedApi == ApiServer.Staging444) -> {
                setPkp = true
            }

            newApi == ApiServer.Sandbox3 || newApi == ApiServer.Staging444 -> {
                setPkp = false
                disablePkp = true
            }
        }

        with(apiServerRepository) {
            setPkp?.let { setPublicKeyPinning(it) }
            changeApi(newApi?.url ?: storedApi.url, disablePkp)
            setNewApi(newApi ?: storedApi)
        }
    }
}