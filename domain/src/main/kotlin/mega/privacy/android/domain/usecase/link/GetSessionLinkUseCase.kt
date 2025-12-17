package mega.privacy.android.domain.usecase.link

import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import javax.inject.Inject

/**
 * Use case
 */
class GetSessionLinkUseCase @Inject constructor(
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase,
) {

    suspend operator fun invoke(link: String): String? =
        if (link.requiresSession()) {
            link.indexOf(SESSION_STRING).let { index ->
                if (index == -1) {
                    null
                } else {
                    link.substring(index + SESSION_STRING.length).takeIf { it.isNotEmpty() }
                        ?.let { path ->
                            runCatching { getSessionTransferURLUseCase(path) }.getOrNull()
                        }
                }
            }
        } else {
            null
        }

    companion object {
        const val SESSION_STRING = "fm/"

        fun String.requiresSession() = contains(SESSION_STRING)
    }
}