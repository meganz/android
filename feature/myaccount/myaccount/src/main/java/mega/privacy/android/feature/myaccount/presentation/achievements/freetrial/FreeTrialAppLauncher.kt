package mega.privacy.android.feature.myaccount.presentation.achievements.freetrial

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

internal const val MEGA_VPN_PACKAGE_NAME = "mega.vpn.android.app"
internal const val MEGA_PASS_PACKAGE_NAME = "mega.pwm.android.app"

internal fun openInSpecificApp(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "market://details?id=$packageName".toUri()
            )
        )
    }.onFailure {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
        )
    }
}
