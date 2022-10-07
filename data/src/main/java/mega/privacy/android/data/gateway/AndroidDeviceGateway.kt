package mega.privacy.android.data.gateway

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * [DeviceGateway] implementation
 *
 * @property context Application context
 */
internal class AndroidDeviceGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceGateway {

    override fun getManufacturerName(): String = Build.MANUFACTURER

    override fun getDeviceModel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            getDeviceModelPost25()
        } else {
            getDeviceModelPre25()
        }

    private fun getDeviceModelPre25(): String = Build.MODEL.apply {
        removePrefix(Build.MANUFACTURER)
        trim()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun getDeviceModelPost25() = Settings.Global.getString(
        context.contentResolver,
        Settings.Global.DEVICE_NAME
    )

    override fun getCurrentDeviceLanguage(): String = Locale.getDefault().displayLanguage
}