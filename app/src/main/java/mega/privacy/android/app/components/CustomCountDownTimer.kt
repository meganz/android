package mega.privacy.android.app.components

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData

/**
 * Class responsible for executing a countdown, reporting each interval and when the countdown has ended
 *
 * @param mutableLiveData MutableLiveData<Boolean> which shall be False while the countdown is in progress and True when the countdown is finished.
 */
class CustomCountDownTimer(var mutableLiveData: MutableLiveData<Boolean>) {
    private var isRunning = false
    lateinit var timer: CountDownTimer

    fun stop() {
        if (this::timer.isInitialized && isRunning) {
            timer.cancel()
            isRunning = false
        }
    }


    fun start(seconds: Long) {
        stop()

        timer = object : CountDownTimer(seconds * 1000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                isRunning = true

                mutableLiveData.postValue(false)
            }

            override fun onFinish() {
                isRunning = false
                mutableLiveData.postValue(true)
            }
        }

        timer.start()
    }
}