package mega.privacy.android.app.usecase.meeting

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Inject

class GetMeetingListUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
) {

    fun get(): Flowable<List<ContactRequestItem>> =
        Flowable.create({ emitter ->
            val chatRooms = megaChatApi.getChatRoomsByType(MegaChatApi.CHAT_TYPE_MEETING_ROOM)
            chatRooms.forEach { chatRoom ->
                chatRoom.chatId
            }

//            emitter.setCancellable {
//                globalSubscription.dispose()
//            }
        }, BackpressureStrategy.LATEST)
}
