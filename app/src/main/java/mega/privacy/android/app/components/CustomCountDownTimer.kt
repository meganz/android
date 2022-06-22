package mega.privacy.android.app.components

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData

/**
 * Class responsible for executing a countdown, reporting each interval and when the countdown has ended
 *
 * @param mutableLiveData MutableLiveData<Boolean> which shall be False while the countdown is in progress and True when the countdown is finished.
 */
class CustomCountDownTimer(var mutableLiveData: MutableLiveData<Boolean>) {
    /**
     * Count down timer
     */
    lateinit var timer: CountDownTimer

    /**
     * Method to stop the counter
     */
    fun stop() {
        if (this::timer.isInitialized) {
            timer.cancel()
            mutableLiveData.postValue(false)
        }
    }

    /**
     * Method to start the counter
     */
    fun start(seconds: Long) {
        stop()

        timer = object : CountDownTimer(seconds * 1000, 1000) {

            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                mutableLiveData.postValue(true)
            }
        }.start()
    }
}