package mega.privacy.android.app.fragments.homepage.main

import android.graphics.Bitmap
import android.util.Log
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

        viewModelScope.launch {
            val defaultAvatar = repository.getDefaultAvatar()
            _avatar.value = defaultAvatar
            loadAvatar()
        }

        avatarChange.observeForever(avatarChangeObserver)
        scrolling.observeForever(scrollingObserver)
    }

    override fun onCleared() {
        super.onCleared()

        repository.unregisterDataListeners()

        avatarChange.removeObserver(avatarChangeObserver)
        scrolling.removeObserver(scrollingObserver)
    }

    private fun loadAvatar() {
        viewModelScope.launch {
            val avatarBitmap = repository.loadAvatar()
            if (avatarBitmap != null) {
                _avatar.value = avatarBitmap
            } else {
                repository.createAvatar(object : DefaultMegaRequestListener {
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
                        }
                    }
                })
            }
        }
    }
}
