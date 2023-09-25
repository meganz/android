package mega.privacy.android.data.mapper.psa

import mega.privacy.android.domain.entity.psa.Psa
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Psa mapper
 *
 */
class PsaMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param request
     * @return Psa
     */
    operator fun invoke(request: MegaRequest) = with(request) {
        Psa(
            id = number.toInt(),
            title = name,
            text = text,
            imageUrl = file,
            positiveText = password,
            positiveLink = link,
            url = email,
        )
    }
}







