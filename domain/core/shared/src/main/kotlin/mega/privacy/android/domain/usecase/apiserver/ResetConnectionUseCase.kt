package mega.privacy.android.domain.usecase.apiserver

import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import javax.inject.Inject

class ResetConnectionUseCase @Inject constructor(
    private val apiServerRepository: ApiServerRepository,
) {
    suspend operator fun invoke(disablePinning: Boolean = false) {
        if (disablePinning) {
            apiServerRepository.setPublicKeyPinning(false)
        }
        apiServerRepository.reconnect()
        apiServerRepository.reconnectFolderApi()
    }
}