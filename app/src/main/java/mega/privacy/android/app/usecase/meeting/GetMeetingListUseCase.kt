package mega.privacy.android.app.usecase.meeting

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_CALL
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatRoom.PRIV_MODERATOR
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import java.io.File
import java.util.Calendar
import java.util.Calendar.DAY_OF_YEAR
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MINUTE
import java.util.Calendar.getInstance
import javax.inject.Inject
import kotlin.random.Random

/**
 * Get meeting list use case
 *
 * @property context                Android context
 * @property megaApi                MegaApi
 * @property megaChatApi            MegaChatApi
 * @property getChatChangesUseCase  Use case needed to get latest changes from Mega Api
 * @property getChatChangesUseCase  Use case needed to get last chat message formatted
 */
class GetMeetingListUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val getChatChangesUseCase: GetChatChangesUseCase,
    private val getLastMessageUseCase: GetLastMessageUseCase,
    private val getFeatureFlag: GetFeatureFlagValue,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    private val hourFormat by lazy { SimpleDateFormat("hh:mm") }
    private var enableScheduleMeetings = false

    init {
        CoroutineScope(defaultDispatcher).launch {
            enableScheduleMeetings = getFeatureFlag(AppFeatures.ScheduleMeeting)
        }
    }

    /**
     * Get a list of updated MeetingItems
     *
     * @return  Flowable
     */
    fun get(): Flowable<List<MeetingItem.Data>> =
        Flowable.create({ emitter ->
            val changesSubscription = CompositeDisposable()
            val meetings = mutableListOf<MeetingItem.Data>()

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        if (request.paramType == MegaApiJava.USER_ATTR_AVATAR && request.file == null) {
                            Timber.w(IllegalArgumentException("Avatar file not available"))
                            return@OptionalMegaRequestListenerInterface
                        }

                        val index = meetings.indexOfFirst {
                            request.nodeHandle == it.firstUser.handle || request.nodeHandle == it.lastUser?.handle
                                    || request.email == it.firstUser.email || request.email == it.lastUser?.email
                                    || request.email == it.firstUser.handle.toString() || request.email == it.lastUser?.handle?.toString()
                        }

                        if (index != Constants.INVALID_POSITION) {
                            val currentItem = meetings[index]
                            when {
                                request.type == MegaRequest.TYPE_GET_USER_EMAIL -> {
                                    meetings[index] =
                                        if (request.nodeHandle == currentItem.firstUser.handle) {
                                            currentItem.copy(
                                                firstUser = currentItem.firstUser.copy(
                                                    email = request.email
                                                )
                                            )
                                        } else {
                                            currentItem.copy(
                                                lastUser = currentItem.lastUser?.copy(
                                                    email = request.email
                                                )
                                            )
                                        }
                                }
                                request.paramType == MegaApiJava.USER_ATTR_FIRSTNAME -> {
                                    meetings[index] =
                                        if (request.email == currentItem.firstUser.handle.toString()) {
                                            currentItem.copy(
                                                firstUser = currentItem.firstUser.copy(
                                                    firstName = request.text
                                                )
                                            )
                                        } else {
                                            currentItem.copy(
                                                lastUser = currentItem.lastUser?.copy(
                                                    firstName = request.text
                                                )
                                            )
                                        }
                                }
                                request.paramType == MegaApiJava.USER_ATTR_AVATAR -> {
                                    meetings[index] =
                                        if (request.email == currentItem.firstUser.email) {
                                            currentItem.copy(
                                                firstUser = currentItem.firstUser.copy(
                                                    avatar = request.file.toUri()
                                                )
                                            )
                                        } else {
                                            currentItem.copy(
                                                lastUser = currentItem.lastUser?.copy(
                                                    avatar = request.file.toUri()
                                                )
                                            )
                                        }
                                }
                            }

                            emitter.onNext(meetings.sortedByDescending(MeetingItem.Data::timeStamp))
                        }
                    } else {
                        Timber.w(error.toMegaException())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    Timber.w(error.toMegaException())
                }
            )

            val muteObserver = Observer<Any> {
                if (emitter.isCancelled) return@Observer
                val index = meetings.indexOfFirst { it.isMuted != !megaApi.isChatNotifiable(it.chatId) }
                if (index != Constants.INVALID_POSITION) {
                    val oldItem = meetings[index]
                    meetings[index] = oldItem.copy(isMuted = !oldItem.isMuted)
                    emitter.onNext(meetings.sortedByDescending(MeetingItem.Data::timeStamp))
                }
            }

            val updateCallObserver = Observer<Any> { chatCall ->
                if (emitter.isCancelled) return@Observer
                val updatedChatId = (chatCall as MegaChatCall).chatid
                val index = meetings.indexOfFirst { it.chatId == updatedChatId }
                if (index != Constants.INVALID_POSITION) {
                    val chatRoom = megaChatApi.getChatRoom(updatedChatId)
                    meetings[index] = chatRoom.toMeetingItem(userAttrsListener)
                    emitter.onNext(meetings.sortedByDescending(MeetingItem.Data::timeStamp))
                }
            }

            megaChatApi.getChatRoomsByType(MegaChatApi.CHAT_TYPE_MEETING_ROOM)
                .filter { !it.isArchived }
                .forEach { chatRoom ->
                    meetings.add(chatRoom.toMeetingItem(userAttrsListener))
                }

            emitter.onNext(meetings.sortedByDescending(MeetingItem.Data::timeStamp))

            getChatChangesUseCase.get()
                .filter { it is Result.OnChatListItemUpdate && it.item != null }
                .subscribeBy(
                    onNext = { change ->
                        if (emitter.isCancelled) return@subscribeBy

                        val updateChange = change as Result.OnChatListItemUpdate
                        val updatedChatId = requireNotNull(updateChange.item).chatId
                        val updatedChatRoom = megaChatApi.getChatRoom(updatedChatId)

                        val index = meetings.indexOfFirst { it.chatId == updatedChatId }
                        if (index != Constants.INVALID_POSITION) {
                            if (updatedChatRoom.isArchived) {
                                meetings.removeAt(index)
                            } else {
                                meetings[index] = updatedChatRoom.toMeetingItem(userAttrsListener)
                            }
                            emitter.onNext(meetings.sortedByDescending(MeetingItem.Data::timeStamp))
                        } else if (updatedChatRoom.isMeeting && !updatedChatRoom.isArchived) {
                            meetings.add(updatedChatRoom.toMeetingItem(userAttrsListener))
                            emitter.onNext(meetings.sortedByDescending(MeetingItem.Data::timeStamp))
                        }
                    },
                    onError = Timber::e
                ).addTo(changesSubscription)

            LiveEventBus.get(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING).observeForever(muteObserver)
            LiveEventBus.get(EVENT_UPDATE_CALL).observeForever(updateCallObserver)

            emitter.setCancellable {
                changesSubscription.dispose()
                LiveEventBus.get(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING).removeObserver(muteObserver)
                LiveEventBus.get(EVENT_UPDATE_CALL).observeForever(updateCallObserver)
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Build MeetingItem given a MegaChatRoom
     *
     * @param listener Listener to deliver user attributes
     * @return                  MeetingItem
     */
    private fun MegaChatRoom.toMeetingItem(listener: OptionalMegaRequestListenerInterface): MeetingItem.Data {
        val chatListItem = megaChatApi.getChatListItem(chatId)

        val title = ChatUtil.getTitleChat(this)
        val isMuted = !megaApi.isChatNotifiable(chatId)
        val hasPermissions = chatListItem.ownPrivilege == PRIV_MODERATOR
        val highlight = unreadCount > 0 || chatListItem.isCallInProgress
                || chatListItem.lastMessageType == MegaChatMessage.TYPE_CALL_STARTED
        val formattedDate = TimeUtils.formatDateAndTime(
            context,
            chatListItem.lastTimestamp,
            TimeUtils.DATE_LONG_FORMAT)
        val firstUser: ContactGroupUser
        var lastUser: ContactGroupUser? = null

        when (peerCount) {
            0L -> {
                firstUser = getGroupUserFromHandle(megaChatApi.myUserHandle, listener)
            }
            1L -> {
                firstUser = getGroupUserFromHandle(megaChatApi.myUserHandle, listener)
                lastUser = getGroupUserFromHandle(getPeerHandle(0), listener)
            }
            else -> {
                firstUser = getGroupUserFromHandle(getPeerHandle(0), listener)
                lastUser = getGroupUserFromHandle(getPeerHandle(peerCount - 1), listener)
            }
        }

        var lastMessageFormatted: String? = null
        getLastMessageUseCase.get(chatId, chatListItem.lastMessageId).blockingSubscribeBy(
            onSuccess = { lastMessageFormatted = it },
            onError = Timber::w
        )
        val isScheduled = enableScheduleMeetings && Random.nextBoolean()
        var isRecurring = false
        val lastMessageIcon = when {
            isScheduled ->
                null
            chatListItem.lastMessageType == MegaChatMessage.TYPE_VOICE_CLIP ->
                R.drawable.ic_mic_on_small
            chatListItem.isGeolocationMetaType() ->
                R.drawable.ic_location_small
            else ->
                null
        }
        var startTime: Calendar? = null
        var endTime: Calendar? = null
        var formattedScheduledTimestamp: String? = null
        if (isScheduled) {
            isRecurring = Random.nextBoolean()
            startTime = getInstance().apply {
                set(DAY_OF_YEAR, get(DAY_OF_YEAR) + Random.nextInt(7))
                set(HOUR_OF_DAY, Random.nextInt(20))
                set(MINUTE, Random.nextInt(60))
            }
            endTime = getInstance().apply {
                timeInMillis = startTime.timeInMillis
                set(HOUR_OF_DAY, get(HOUR_OF_DAY) + Random.nextInt(1, 4))
            }
            val scheduleText = "${hourFormat.format(startTime.time)} - ${hourFormat.format(endTime.time)}"
            formattedScheduledTimestamp = if (isRecurring) {
                StringResourcesUtils.getString(R.string.meetings_list_scheduled_meeting_weekly_label, scheduleText)
            } else {
                scheduleText
            }
        }

        return MeetingItem.Data(
            chatId = chatId,
            title = title,
            lastMessage = lastMessageFormatted,
            lastMessageIcon = lastMessageIcon,
            isPublic = isPublic,
            isMuted = isMuted,
            hasPermissions = hasPermissions,
            firstUser = firstUser,
            lastUser = lastUser,
            unreadCount = unreadCount,
            highlight = highlight,
            timeStamp = chatListItem.lastTimestamp,
            formattedTimestamp = formattedDate,
            formattedScheduledTimestamp = formattedScheduledTimestamp,
            startTimestamp = startTime?.timeInMillis,
            endTimestamp = endTime?.timeInMillis,
            isRecurring
        )
    }

    /**
     * Build ContactGroupUser given an User handle
     *
     * @param userHandle    User handle to obtain group
     * @param listener      Listener to deliver user attributes
     * @return              ContactGroupUser
     */
    private fun getGroupUserFromHandle(
        userHandle: Long,
        listener: OptionalMegaRequestListenerInterface,
    ): ContactGroupUser {
        val myself = userHandle == megaChatApi.myUserHandle
        var userAvatar: File? = null
        val userName = if (myself) {
            megaChatApi.myFirstname
        } else {
            megaChatApi.getUserFirstnameFromCache(userHandle)
        }
        val userEmail = if (myself) {
            megaChatApi.myEmail
        } else {
            megaChatApi.getUserEmailFromCache(userHandle)
        }
        val userAvatarColor = megaApi.getUserAvatarColor(userHandle.toString()).toColorInt()

        if (userName.isNullOrBlank()) {
            megaApi.getUserAttribute(userHandle.toString(),
                MegaApiJava.USER_ATTR_FIRSTNAME,
                listener)
        }

        if (userEmail.isNullOrBlank()) {
            megaApi.getUserEmail(userHandle, listener)
        } else {
            val avatarFile = AvatarUtil.getUserAvatarFile(context, userEmail)
            if (avatarFile?.exists() == true) {
                userAvatar = avatarFile
            } else {
                megaApi.getUserAvatar(userEmail, avatarFile?.absolutePath, listener)
            }
        }

        return ContactGroupUser(
            handle = userHandle,
            email = userEmail,
            firstName = userName,
            avatar = userAvatar?.toUri(),
            avatarColor = userAvatarColor
        )
    }

    private fun MegaChatListItem.isGeolocationMetaType(): Boolean =
        lastMessageType == MegaChatMessage.TYPE_CONTAINS_META
                && megaChatApi.getMessage(chatId, lastMessageId)
            ?.containsMeta?.type == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION
}
