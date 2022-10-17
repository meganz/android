package mega.privacy.android.data.gateway

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


/**
 * Default implementation of [AppInfoGateway]
 *
 */
internal class DefaultAppInfoGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppInfoGateway {
    // getPackageInfo is expensive we don't need to query every call we should cache it
    private val applicationCode by lazy {
        runCatching {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName,
                    PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            // PackageInfoCompat supports both getLongVersionCode and versionCode
            return@runCatching PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        }.getOrDefault(0)
    }

    override fun getAppVersionCode(): Int = applicationCode
}