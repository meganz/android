package mega.privacy.android.domain.usecase.link

import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import javax.inject.Inject

/**
 * Use case
 */
class GetSessionLinkUseCase @Inject constructor(
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase,
) {

    private val sessionString = "fm/"

    suspend operator fun invoke(link: String): String? =
        if (link.contains(sessionString)) {
            link.indexOf(sessionString).let { index ->
                if (index == -1) {
                    null
                } else {
                    link.substring(index + sessionString.length).takeIf { it.isNotEmpty() }
                        ?.let { path ->
                            runCatching { getSessionTransferURLUseCase(path) }.getOrNull()
                        }
                }
            }
        } else {
            null
        }
}