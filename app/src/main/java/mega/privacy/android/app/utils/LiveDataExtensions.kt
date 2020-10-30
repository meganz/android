package mega.privacy.android.app.utils

import androidx.lifecycle.MutableLiveData

/**
 * Notify observer with the latest non-null value.
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}
