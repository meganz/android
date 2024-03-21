package mega.privacy.android.app.service.installreferrer

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerDetails
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerHandler
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [InstallReferrerHandler] Implementation
 */
class InstallReferrerHandlerImpl @Inject constructor(
    @ApplicationContext context: Context,
) : InstallReferrerHandler {

    private val referrerClient: InstallReferrerClient by lazy {
        InstallReferrerClient.newBuilder(context).build()
    }

    override suspend fun getDetails(): InstallReferrerDetails =
        suspendCancellableCoroutine { continuation ->
            val listener = object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            val response = referrerClient.installReferrer
                            continuation.resume(
                                InstallReferrerDetails(
                                    response.installReferrer,
                                    response.referrerClickTimestampSeconds,
                                    response.installBeginTimestampSeconds
                                )
                            )
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                            continuation.resumeWithException(
                                IllegalStateException("Install Referrer API not available")
                            )
                        }

                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            continuation.resumeWithException(
                                IOException("Install Referrer API connection to service unavailable")
                            )
                        }

                        else -> {
                            continuation.resumeWithException(
                                IOException("Install Referrer API response code: $responseCode")
                            )
                        }
                    }
                    referrerClient.endConnection() // Disconnect after receiving response
                }

                override fun onInstallReferrerServiceDisconnected() {
                    continuation.resumeWithException(IOException("Install Referrer API service disconnected"))
                }
            }

            referrerClient.startConnection(listener)

            continuation.invokeOnCancellation {
                referrerClient.endConnection() // Disconnect on cancellation
            }
        }
}