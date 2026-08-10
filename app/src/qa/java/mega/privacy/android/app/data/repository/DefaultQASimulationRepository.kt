package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.domain.repository.QASimulationRepository
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Default implementation of [QASimulationRepository]. QA only.
 */
class DefaultQASimulationRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QASimulationRepository {

    /**
     * Writes the dev option purge payload to USER_ATTR_DEV_OPT to simulate the purge schedule.
     */
    override suspend fun setDevOptForPurge(
        purgeTimestamp: Long,
        reason: Int,
        warningTimestamp: Long,
        lastActiveTimestamp: Long,
    ) = withContext(ioDispatcher) {
        val value = buildDevOptForPurge(
            purgeTimestamp = purgeTimestamp,
            reason = reason,
            warningTimestamp = warningTimestamp,
            lastActiveTimestamp = lastActiveTimestamp,
        )
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.failWithError(error, "setDevOptForPurge")
                    }
                }
            )
            megaApiGateway.setUserAttribute(
                type = MegaApiJava.USER_ATTR_DEV_OPT,
                value = value,
                listener = listener,
            )
        }
    }

    /**
     * Reads the last acknowledged purge timestamp from USER_ATTR_LAST_PURGE_ACKNOWLEDGED (0 if unset).
     */
    override suspend fun getLastPurgeAcknowledged(): Long = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        // For USER_ATTR_LAST_PURGE_ACKNOWLEDGED the SDK parses the value into
                        // MegaRequest.number (0 if it has not been set).
                        MegaError.API_OK -> continuation.resumeWith(Result.success(request.number))
                        MegaError.API_ENOENT -> continuation.resumeWith(Result.success(0L))
                        else -> continuation.failWithError(error, "getLastPurgeAcknowledged")
                    }
                }
            )
            megaApiGateway.getUserAttribute(
                MegaApiJava.USER_ATTR_LAST_PURGE_ACKNOWLEDGED,
                listener,
            )
        }
    }

    /**
     * Builds the `{"lastpurge":[...]}` JSON payload, appending warning/last-active only when present.
     */
    private fun buildDevOptForPurge(
        purgeTimestamp: Long,
        reason: Int,
        warningTimestamp: Long,
        lastActiveTimestamp: Long,
    ): String {
        val lastPurge = JSONArray().apply {
            put(purgeTimestamp)
            put(reason)
            if (warningTimestamp > 0) {
                put(warningTimestamp)
                if (lastActiveTimestamp > 0) {
                    put(lastActiveTimestamp)
                }
            }
        }
        return JSONObject().put("lastpurge", lastPurge).toString()
    }
}
