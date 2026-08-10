package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.LinksRepository
import javax.inject.Inject

/**
 * Implementation of [LinksRepository]
 */
internal class LinksRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LinksRepository {

    override suspend fun decryptPasswordProtectedLink(
        passwordProtectedLink: String,
        password: String,
    ): String? =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("decryptPasswordProtectedLink") {
                    it.text
                }
                megaApiGateway.decryptPasswordProtectedLink(
                    passwordProtectedLink,
                    password,
                    listener
                )
            }
        }
}

