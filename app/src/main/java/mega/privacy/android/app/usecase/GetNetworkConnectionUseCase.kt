package mega.privacy.android.app.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.utils.NetworkUtil.isOnline
import javax.inject.Inject

/**
 * Main use case to get information about current Internet connectivity
 *
 * @property context    Context required to get Connectivity Manager
 */
class GetNetworkConnectionUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Registers to receive notifications about all networks when Internet is available or not.
     *
     * @return  Flowable returning true if it has Internet access, false otherwise.
     */
    fun getConnectionUpdates(): Flowable<Boolean> =
        Flowable.create({ emitter ->
            emitter.onNext(context.isOnline())

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_INTERNET)
                .build()

            val listener = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    emitter.onNext(true)
                }

                override fun onLost(network: Network) {
                    emitter.onNext(false)
                }

                override fun onUnavailable() {
                    emitter.onNext(false)
                }
            }

            connectivityManager.registerNetworkCallback(networkRequest, listener)
            emitter.setCancellable {
                connectivityManager.unregisterNetworkCallback(listener)
            }
        }, BackpressureStrategy.LATEST)
}
