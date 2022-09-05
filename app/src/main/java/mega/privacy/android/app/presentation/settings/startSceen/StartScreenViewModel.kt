package mega.privacy.android.app.presentation.settings.startSceen

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_START_SCREEN
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.HOME_BNV
import mega.privacy.android.app.utils.SharedPreferenceConstants.DO_NOT_ALERT_ABOUT_START_SCREEN
import mega.privacy.android.app.utils.SharedPreferenceConstants.PREFERRED_START_SCREEN

class StartScreenViewModel : BaseRxViewModel() {

    private val checkedScreen = MutableLiveData<Int>()

    fun onScreenChecked(): LiveData<Int> = checkedScreen

    private lateinit var preferences: SharedPreferences

    fun initPreferences(preferences: SharedPreferences) {
        this.preferences = preferences
        checkedScreen.value = preferences.getInt(PREFERRED_START_SCREEN, HOME_BNV)
    }

    fun newScreenClicked(newScreen: Int) {
        if (newScreen == checkedScreen.value) {
            return
        }

        LiveEventBus.get(EVENT_UPDATE_START_SCREEN, Int::class.java).post(newScreen)
        preferences.edit()
            .putInt(PREFERRED_START_SCREEN, newScreen)
            .putBoolean(DO_NOT_ALERT_ABOUT_START_SCREEN, true)
            .apply()
        checkedScreen.value = newScreen
    }
}