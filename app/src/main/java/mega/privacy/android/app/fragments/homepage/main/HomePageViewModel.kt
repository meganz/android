package mega.privacy.android.app.fragments.homepage.main

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.*
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val repository: HomepageRepository
) : ViewModel() {

    private val _notificationCount = MutableLiveData<Int>()
    private val _avatar = MutableLiveData<Bitmap>()
    private val _chatStatus = MutableLiveData<Int>()
    private val _callStatus = MutableLiveData<Int>()

    private val _bannerList: MutableLiveData<MutableList<MegaBanner>?> =
        repository.getBannerListLiveData()

    val notificationCount: LiveData<Int> = _notificationCount
    val avatar: LiveData<Bitmap> = _avatar
    val chatStatus: LiveData<Int> = _chatStatus
    val callStatus: LiveData<Int> = _callStatus

    val bannerList: LiveData<MutableList<MegaBanner>?> = _bannerList

    private val avatarChangeObserver = androidx.lifecycle.Observer<Boolean> {
        loadAvatar()
    }

    private val notificationCountObserver = androidx.lifecycle.Observer<Int> {
        _notificationCount.value = it
    }

    private val chatOnlineStatusObserver = androidx.lifecycle.Observer<Int> {
        _chatStatus.value = it
    }

    private val callStatusObserver = androidx.lifecycle.Observer<MegaChatCall> {
        _callStatus.value = it.status
    }

    init {
        LiveEventBus.get(EVENT_AVATAR_CHANGE, Boolean::class.java)
            .observeForever(avatarChangeObserver)
        LiveEventBus.get(EVENT_NOTIFICATION_COUNT_CHANGE, Int::class.java)
            .observeForever(notificationCountObserver)
        LiveEventBus.get(EVENT_CHAT_STATUS_CHANGE, Int::class.java)
            .observeForever(chatOnlineStatusObserver)
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeForever(callStatusObserver)

        // Show the default avatar (the Alphabet avatar) above all, then load the actual avatar
        showDefaultAvatar().invokeOnCompletion {
            loadAvatar(true)
        }
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_AVATAR_CHANGE, Boolean::class.java)
            .removeObserver(avatarChangeObserver)
        LiveEventBus.get(EVENT_NOTIFICATION_COUNT_CHANGE, Int::class.java)
            .removeObserver(notificationCountObserver)
        LiveEventBus.get(EVENT_CHAT_STATUS_CHANGE, Int::class.java)
            .removeObserver(chatOnlineStatusObserver)
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .removeObserver(callStatusObserver)
    }

    private fun showDefaultAvatar() = viewModelScope.launch {
        _avatar.value = repository.getDefaultAvatar()
    }

    /**
     * Generate and show the round avatar based on the actual avatar stored in the cache folder.
     * Try to retrieve the avatar from the server if it has not been cached.
     * Showing the default avatar if the retrieve failed
     */
    private fun loadAvatar(retry: Boolean = false) {
        viewModelScope.launch {
            repository.loadAvatar()?.also {
                when {
                    it.first -> _avatar.value = it.second
                    retry -> repository.createAvatar(object :
                        BaseListener(MegaApplication.getInstance()) {
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

    /**
     * Get banner list from the server or from memory cache
     */
    fun getBanners() {
        viewModelScope.launch { repository.loadBannerList() }
    }

    /**
     * Dismiss the banner for this account.
     * The banner would never be given again by the server once being dismissed
     */
    fun dismissBanner(banner: MegaBanner) {
        repository.dismissBanner(banner.id)
        _bannerList.value?.remove(banner)
        _bannerList.value?.let {
            _bannerList.value = it
        }
    }
}
