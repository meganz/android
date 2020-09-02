package mega.privacy.android.app.arch

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class BaseRxViewModel : ViewModel() {

    protected var composite = CompositeDisposable()

    protected fun add(disposable: Disposable?) {
        composite.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        composite.clear()
    }
}
