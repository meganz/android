package mega.privacy.android.app.fragments.settingsFragments.startSceen

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_START_SCREEN
import mega.privacy.android.app.constants.SettingsConstants.DEFAULT_PREFERRED_START_SCREEN
import mega.privacy.android.app.constants.SettingsConstants.PREFERRED_START_SCREEN

class StartScreenViewModel : BaseRxViewModel() {

    private val checkedScreen = MutableLiveData<Int>()

    fun onScreenChecked(): LiveData<Int> = checkedScreen

    private lateinit var preferences: SharedPreferences

    fun initPreferences(preferences: SharedPreferences) {
        this.preferences = preferences
        checkedScreen.value =
            preferences.getInt(PREFERRED_START_SCREEN, DEFAULT_PREFERRED_START_SCREEN)
    }

    fun newScreenClicked(newScreen: Int) {
        if (newScreen == checkedScreen.value) {
            return
        }

        LiveEventBus.get(EVENT_UPDATE_START_SCREEN, Int::class.java).post(newScreen)
        preferences.edit().putInt(PREFERRED_START_SCREEN, newScreen).apply()
        checkedScreen.value = newScreen
    }
}