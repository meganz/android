package mega.privacy.android.app.arch

import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.MegaApplication

abstract class BaseRxViewModel : AndroidViewModel(MegaApplication.getInstance()) {
  protected var composite = CompositeDisposable()
  protected fun add(disposable: Disposable?) {
    composite.add(disposable)
  }

  override fun onCleared() {
    super.onCleared()
    composite.clear()
  }
}