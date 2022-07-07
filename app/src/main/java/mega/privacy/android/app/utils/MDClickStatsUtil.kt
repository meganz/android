package mega.privacy.android.app.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.SharedPreferenceConstants.MEDIA_DISCOVERY_CLICK
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class MDClickStatsUtil @Inject constructor(

) {
    companion object {
        private const val CLICK_COUNT = "ClickCount"
        private const val CLICK_COUNT_FOLDER = "ClickCountFolder"

        /**
         * Fire off the stats events
         *
         * @param context       The context
         * @param mediaHandle   The folder which the click is happening
         */
        fun fireStatsEvent(megaApi: MegaApiAndroid, context: Context, mediaHandle: Long) {
            //TODO this method needs to be refactored to clean architecture

            CoroutineScope(Dispatchers.IO).launch {
                val sharedPreference: SharedPreferences = context.getSharedPreferences(
                    MEDIA_DISCOVERY_CLICK,
                    Context.MODE_PRIVATE
                )

                var clickCount = sharedPreference.getInt(CLICK_COUNT, 0)
                var clickCountFolder = sharedPreference.getInt(CLICK_COUNT_FOLDER + mediaHandle, 0)
                val editor = sharedPreference.edit()
                editor.putInt(CLICK_COUNT, (++clickCount))
                editor.putInt(CLICK_COUNT_FOLDER + mediaHandle, (++clickCountFolder))
                editor.apply()
                megaApi.sendEvent(
                    Constants.STATS_MD_CLICK,
                    "Media Discovery Click"
                )
                if (clickCount > 3) {
                    megaApi.sendEvent(
                        Constants.STATS_MD_CLICK_MORE_THAN_3,
                        "Media Discovery Click > 3"
                    )
                }
                if (clickCountFolder > 3) {
                    megaApi.sendEvent(
                        Constants.STATS_MD_CLICK_MORE_THAN_3_SAME_FOLDER,
                        "Media Discovery Click Specific Folder > 3"
                    )
                }
            }
        }
    }
}