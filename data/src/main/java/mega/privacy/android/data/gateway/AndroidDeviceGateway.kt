package mega.privacy.android.data.gateway

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.extensions.registerReceiverAsFlow
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber
import java.net.NetworkInterface
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

/**
 * [DeviceGateway] implementation
 *
 * @property context Application context
 */
internal class AndroidDeviceGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
) : DeviceGateway {

    override fun getManufacturerName(): String = Build.MANUFACTURER

    override fun getDeviceModel(): String = getDeviceModelPost25()

    private fun getDeviceModelPost25() = Settings.Global.getString(
        context.contentResolver,
        Settings.Global.DEVICE_NAME
    )

    override fun getCurrentDeviceLanguage(): String = Locale.getDefault().displayLanguage

    override suspend fun getSdkVersionInt() = Build.VERSION.SDK_INT

    override suspend fun getSdkVersionName(): String = "Android ${Build.VERSION.RELEASE}"

    override fun getCurrentTimeInMillis(): Long = System.currentTimeMillis()

    override fun getElapsedRealtime(): Long = SystemClock.elapsedRealtime()

    override suspend fun getDeviceMemory(): Long? {
        val activityManager =
            context.getSystemService(ActivityManager::class.java) ?: return null
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }

    override suspend fun getDiskSpaceBytes(path: String) = with(StatFs(path)) {
        availableBlocksLong * blockSizeLong
    }

    override fun is24HourFormat(): Boolean =
        DateFormat.is24HourFormat(context)

    override val now: Long
        get() = System.currentTimeMillis()

    override val nanoTime: Long
        get() = System.nanoTime()

    override suspend fun isCharging(): Boolean {
        val batteryIntent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        return status == BatteryManager.BATTERY_PLUGGED_AC || status == BatteryManager.BATTERY_PLUGGED_USB || status == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }

    override suspend fun getLocalIpAddress(): String? {
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val interfaceName = networkInterface.name
                // Ensure get the IP from the current active network interface
                val connectivityManager =
                    context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeInterfaceName =
                    connectivityManager.getLinkProperties(connectivityManager.activeNetwork)?.interfaceName
                if (activeInterfaceName != null && interfaceName.compareTo(activeInterfaceName) != 0) {
                    continue
                }
                val enumIpAddress = networkInterface.inetAddresses
                while (enumIpAddress.hasMoreElements()) {
                    val inetAddress = enumIpAddress.nextElement()
                    if (inetAddress != null && !inetAddress.isLoopbackAddress) {
                        return inetAddress.hostAddress
                    }
                }
            }
        }.onFailure {
            Timber.e(it, "Error getting local IP address")
        }
        return null
    }

    override fun getCurrentHourOfDay(): Int = LocalTime.now().hour

    override fun getCurrentMinute(): Int = LocalTime.now().minute

    override val monitorBatteryInfo =
        context.registerReceiverAsFlow(
            flags = ContextCompat.RECEIVER_EXPORTED,
            Intent.ACTION_BATTERY_CHANGED,
        ).map {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val status: Int = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val isCharging =
                status == BatteryManager.BATTERY_PLUGGED_AC || status == BatteryManager.BATTERY_PLUGGED_USB || status == BatteryManager.BATTERY_PLUGGED_WIRELESS
            return@map BatteryInfo(level = level, isCharging = isCharging)
        }.catch {
            Timber.e(it, "MonitorBatteryInfo Exception")
        }.toSharedFlow(appScope)

    override val monitorChargingStoppedState =
        context.registerReceiverAsFlow(
            flags = ContextCompat.RECEIVER_EXPORTED,
            Intent.ACTION_POWER_DISCONNECTED,
        ).map {
            Timber.d("Charging Stopped")
            true
        }.catch {
            Timber.e(it, "MonitorChargingStoppedState Exception")
        }.toSharedFlow(appScope)
}

private fun <T> Flow<T>.toSharedFlow(
    scope: CoroutineScope,
) = shareIn(scope, started = SharingStarted.WhileSubscribed())
