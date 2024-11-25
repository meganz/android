package mega.privacy.android.app.presentation.psa.mapper

import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.entity.psa.Psa
import javax.inject.Inject

/**
 * Psa state mapper
 */
class PsaStateMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param psa
     */
    operator fun invoke(psa: Psa?) = listOf(
        ::createWebPsa,
        ::createStandardPsa,
        ::createInfoPsa,
    ).firstNotNullOfOrNull { it(psa) } ?: PsaState.NoPsa

    private fun createWebPsa(
        psa: Psa?,
    ) = psa?.url?.takeUnless { it.isBlank() }?.let { PsaState.WebPsa(psa.id, it) }

    private fun createStandardPsa(psa: Psa?) =
        psa?.let {
            val positiveText = psa.positiveText ?: return@let null
            val positiveLink = psa.positiveLink ?: return@let null
            PsaState.StandardPsa(
                psa.id,
                psa.title,
                psa.text,
                psa.imageUrl,
                positiveText,
                positiveLink
            )
        }

    private fun createInfoPsa(psa: Psa?) =
        psa?.let {
            PsaState.InfoPsa(
                psa.id,
                psa.title,
                psa.text,
                psa.imageUrl,
            )
        }
}