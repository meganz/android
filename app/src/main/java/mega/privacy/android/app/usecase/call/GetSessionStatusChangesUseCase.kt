package mega.privacy.android.app.usecase.call

import android.util.Pair
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.constants.EventConstants

import nz.mega.sdk.*
import javax.inject.Inject

/**
 * Main use case to get changes in session status
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 */
class GetSessionStatusChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    /**
     * Participants' changes result
     *
     * @property call               MegaChatCall
     * @property sessionStatus      Status of the session
     * @property isRecoverable      True, if termCode is SESS_TERM_CODE_RECOVERABLE, false if termCode is SESS_TERM_CODE_NON_RECOVERABLE
     * @property lastParticipantPeerId   Peer ID of last participant in the call
     */
    data class SessionChangedResult(
        val call: MegaChatCall?,
        val sessionStatus: Int,
        val isRecoverable: Boolean?,
        val lastParticipantPeerId: Long
    )

    /**
     * Method to get changes in session
     *
     * @return Flowable containing the call, session status, if it's recoverable, the last participant peerId
     */
    fun getSessionChanged(): Flowable<SessionChangedResult> =
        Flowable.create({ emitter ->
            val sessionStatusObserver =
                Observer<Pair<MegaChatCall?, MegaChatSession>> { callAndSession ->
                    val session = callAndSession.second as MegaChatSession
                    val call = callAndSession.first
                    val peerId: Long = session.peerid
                    var isRecoverable: Boolean? = null

                    when (val sessionStatus = session.status) {
                        MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                            emitter.onNext(
                                SessionChangedResult(
                                    call,
                                    sessionStatus,
                                    isRecoverable,
                                    peerId
                                )
                            )
                        }
                        MegaChatSession.SESSION_STATUS_DESTROYED -> {
                            when (session.termCode) {
                                MegaChatSession.SESS_TERM_CODE_NON_RECOVERABLE -> isRecoverable =
                                    false
                                MegaChatSession.SESS_TERM_CODE_RECOVERABLE -> isRecoverable = true
                            }

                            emitter.onNext(
                                SessionChangedResult(
                                    call,
                                    sessionStatus,
                                    isRecoverable,
                                    peerId
                                )
                            )
                        }
                    }
                }

            @Suppress("UNCHECKED_CAST")
            LiveEventBus.get(EventConstants.EVENT_SESSION_STATUS_CHANGE)
                .observeForever(sessionStatusObserver as Observer<Any>)

            emitter.setCancellable {
                @Suppress("UNCHECKED_CAST")
                LiveEventBus.get(EventConstants.EVENT_SESSION_STATUS_CHANGE)
                    .removeObserver(sessionStatusObserver as Observer<Any>)

            }
        }, BackpressureStrategy.LATEST)
}