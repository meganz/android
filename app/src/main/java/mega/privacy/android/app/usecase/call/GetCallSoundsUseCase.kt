package mega.privacy.android.app.usecase.call

import android.util.Pair
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT

import nz.mega.sdk.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Main use case to control when a call-related sound should be played.
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 * @property getParticipantsChangesUseCase GetParticipantsChangesUseCase
 */
class GetCallSoundsUseCase @Inject constructor(
        private val megaChatApi: MegaChatApiAndroid,
        private val getParticipantsChangesUseCase: GetParticipantsChangesUseCase
) {

    fun get(): Flowable<CallSoundType> =
            Flowable.create({ emitter ->
                val disposable = CompositeDisposable()

                val sessionStatusObserver = Observer<Pair<MegaChatCall?, MegaChatSession>> { callAndSession ->
                    val session = callAndSession.second as MegaChatSession
                    val call = callAndSession.first
                    if (session.status == MegaChatSession.SESSION_STATUS_DESTROYED && session.termCode == MegaChatSession.SESS_TERM_CODE_NON_RECOVERABLE) {
                        if (call == null) {
                            emitter.onNext(CallSoundType.CALL_ENDED)
                        } else {
                            megaChatApi.getChatRoom(call.chatid)?.let { chat ->
                                if (!chat.isGroup && !chat.isMeeting) {
                                    emitter.onNext(CallSoundType.CALL_ENDED)
                                }
                            }
                        }
                    }
                }

                @Suppress("UNCHECKED_CAST")
                LiveEventBus.get(EventConstants.EVENT_SESSION_STATUS_CHANGE)
                        .observeForever(sessionStatusObserver as Observer<Any>)

                getParticipantsChangesUseCase.getChangesFromParticipants()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                                onNext = { result ->
                                    when (result.typeChange) {
                                        TYPE_JOIN -> emitter.onNext(CallSoundType.PARTICIPANT_JOINED_CALL)
                                        TYPE_LEFT -> emitter.onNext(CallSoundType.PARTICIPANT_LEFT_CALL)
                                    }
                                },
                                onError = { error ->
                                    Timber.e(error.stackTraceToString())
                                }
                        )
                        .addTo(disposable)

                emitter.setCancellable {
                    @Suppress("UNCHECKED_CAST")
                    LiveEventBus.get(EventConstants.EVENT_SESSION_STATUS_CHANGE)
                            .removeObserver(sessionStatusObserver as Observer<Any>)

                    disposable.clear()
                }
            }, BackpressureStrategy.LATEST)
}

