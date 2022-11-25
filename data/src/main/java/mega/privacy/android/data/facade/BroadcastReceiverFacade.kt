package mega.privacy.android.data.facade

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.extensions.registerReceiverAsFlow
import mega.privacy.android.data.gateway.BroadcastReceiverGateway
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject

internal class BroadcastReceiverFacade @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : BroadcastReceiverGateway {

    override val monitorBatteryInfo =
        context.registerReceiverAsFlow(Intent.ACTION_BATTERY_CHANGED).map {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val status: Int = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val isCharging =
                status == BatteryManager.BATTERY_PLUGGED_AC || status == BatteryManager.BATTERY_PLUGGED_USB || status == BatteryManager.BATTERY_PLUGGED_WIRELESS
            return@map BatteryInfo(level = level, isCharging = isCharging)
        }.toSharedFlow(appScope)

    override val monitorChargingStoppedState =
        context.registerReceiverAsFlow(Intent.ACTION_POWER_DISCONNECTED).map {
            true
        }.toSharedFlow(appScope)
}

private fun <T> Flow<T>.toSharedFlow(
    scope: CoroutineScope,
) = shareIn(scope, started = SharingStarted.WhileSubscribed())
