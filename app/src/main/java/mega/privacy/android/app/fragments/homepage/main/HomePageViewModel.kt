package mega.privacy.android.app.fragments.homepage.main

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.fragments.homepage.avatarChange
import mega.privacy.android.app.fragments.homepage.scrolling
import mega.privacy.android.app.listeners.DefaultMegaRequestListener
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class HomePageViewModel @ViewModelInject constructor(
    private val repository: HomepageRepository
) : ViewModel() {
    private val _avatar = MutableLiveData<Bitmap>()
    private val _isScrolling = MutableLiveData<Pair<Scrollable, Boolean>>()

    val notification: LiveData<Int> = repository.getNotificationLiveData()
    val avatar: LiveData<Bitmap> = _avatar
    val chatStatus: LiveData<Int> = repository.getChatStatusLiveData()
    val isScrolling: LiveData<Pair<Scrollable, Boolean>> = _isScrolling

    private val avatarChangeObserver = androidx.lifecycle.Observer<Boolean> {
        loadAvatar()
    }

    private val scrollingObserver = androidx.lifecycle.Observer<Pair<Scrollable, Boolean>> {
        _isScrolling.value = it
    }

    init {
        repository.registerDataListeners()

        avatarChange.observeForever(avatarChangeObserver)
        scrolling.observeForever(scrollingObserver)

        showDefaultAvatar().invokeOnCompletion {
            loadAvatar(true)
        }
    }

    override fun onCleared() {
        super.onCleared()

        repository.unregisterDataListeners()

        avatarChange.removeObserver(avatarChangeObserver)
        scrolling.removeObserver(scrollingObserver)
    }

    private fun showDefaultAvatar() = viewModelScope.launch {
        _avatar.value = repository.getDefaultAvatar()
    }

    private fun loadAvatar(retry: Boolean = false) {
        viewModelScope.launch {
            repository.loadAvatar()?.also {
                when {
                    it.first -> _avatar.value = it.second
                    retry -> repository.createAvatar(object : DefaultMegaRequestListener {
                        override fun onRequestFinish(
                            api: MegaApiJava,
                            request: MegaRequest,
                            e: MegaError
                        ) {
                            if (request.type == MegaRequest.TYPE_GET_ATTR_USER
                                && request.paramType == MegaApiJava.USER_ATTR_AVATAR
                                && e.errorCode == MegaError.API_OK
                            ) {
                                loadAvatar()
                            } else {
                                showDefaultAvatar()
                            }
                        }
                    })
                    else -> showDefaultAvatar()
                }
            }
        }
    }
}
