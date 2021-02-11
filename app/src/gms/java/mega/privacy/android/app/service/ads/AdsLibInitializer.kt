package mega.privacy.android.app.service.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds

object AdsLibInitializer {

    fun init(context: Context) {
        MobileAds.initialize(context)
    }
}