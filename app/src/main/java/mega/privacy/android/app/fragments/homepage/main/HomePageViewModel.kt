package mega.privacy.android.app.fragments.homepage.main

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.listeners.BaseListener
import nz.mega.sdk.*

class HomePageViewModel @ViewModelInject constructor(
    private val repository: HomepageRepository
) : ViewModel() {
    private var lastGetBannerTime = 0L

    private val _notificationCount = MutableLiveData<Int>()
    private val _avatar = MutableLiveData<Bitmap>()
    private val _chatStatusDrawableId = MutableLiveData<Int>()
    private val _isScrolling = MutableLiveData<Pair<Scrollable, Boolean>>()
    private val _bannerList: MutableLiveData<MutableList<MegaBanner>?> = repository.getBannerListLiveData()

    val notificationCount: LiveData<Int> = _notificationCount
    val avatar: LiveData<Bitmap> = _avatar
    val chatStatusDrawableId: LiveData<Int> = _chatStatusDrawableId
    val isScrolling: LiveData<Pair<Scrollable, Boolean>> = _isScrolling
    val bannerList: LiveData<MutableList<MegaBanner>?> = _bannerList

    private val avatarChangeObserver = androidx.lifecycle.Observer<Boolean> {
        loadAvatar()
    }

    private val scrollingObserver = androidx.lifecycle.Observer<Pair<Scrollable, Boolean>> {
        _isScrolling.value = it
    }

    private val notificationCountObserver = androidx.lifecycle.Observer<Int> {
        _notificationCount.value = it
    }

    private val chatOnlineStatusObserver = androidx.lifecycle.Observer<Int> {
        _chatStatusDrawableId.value = repository.getChatStatusDrawableId(it)
    }

    init {
        avatarChange.observeForever(avatarChangeObserver)
        scrolling.observeForever(scrollingObserver)
        notificationCountChange.observeForever(notificationCountObserver)
        chatOnlineStatusChange.observeForever(chatOnlineStatusObserver)

        showDefaultAvatar().invokeOnCompletion {
            loadAvatar(true)
        }
    }

    override fun onCleared() {
        super.onCleared()

        avatarChange.removeObserver(avatarChangeObserver)
        scrolling.removeObserver(scrollingObserver)
        notificationCountChange.removeObserver(notificationCountObserver)
        chatOnlineStatusChange.removeObserver(chatOnlineStatusObserver)
    }

    private fun showDefaultAvatar() = viewModelScope.launch {
        _avatar.value = repository.getDefaultAvatar()
    }

    private fun loadAvatar(retry: Boolean = false) {
        viewModelScope.launch {
            repository.loadAvatar()?.also {
                when {
                    it.first -> _avatar.value = it.second
                    retry -> repository.createAvatar(object : BaseListener(MegaApplication.getInstance()) {
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

    fun isRootNodeNull() = repository.isRootNodeNull()

    fun updateBannersIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGetBannerTime > 1000) {
            lastGetBannerTime = currentTime
            viewModelScope.launch { repository.loadBannerList() }
        }
    }

    fun dismissBanner(banner: MegaBanner) {
        repository.dismissBanner(banner.id)
        _bannerList.value?.remove(banner)
        _bannerList.value?.let {
            _bannerList.value = it
        }
    }
}
