package mega.privacy.android.app.data.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeOnce(observer: Observer<T?>) {
    observeForever(object : Observer<T?> {
        override fun onChanged(value: T?) {
            observer.onChanged(value)
            removeObserver(this)
        }
    })
}