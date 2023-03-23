package mega.privacy.android.app.usecase.call

import android.util.Pair
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.constants.EventConstants
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession
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
     * @property peerId             Peer ID of participant
     * @property clientId           Client ID of participant
     */
    data class SessionChangedResult(
        val call: MegaChatCall?,
        val sessionStatus: Int,
        val isRecoverable: Boolean?,
        val peerId: Long,
        val clientId: Long
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
                    val clientId: Long = session.clientid
                    var isRecoverable: Boolean? = null

                    when (val sessionStatus = session.status) {
                        MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                            emitter.onNext(
                                SessionChangedResult(
                                    call,
                                    sessionStatus,
                                    isRecoverable,
                                    peerId,
                                    clientId
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
                                    peerId,
                                    clientId
                                )
                            )
                        }
                    }
                }

            LiveEventBus.get<Pair<MegaChatCall?, MegaChatSession>>(EventConstants.EVENT_SESSION_STATUS_CHANGE)
                .observeForever(sessionStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get<Pair<MegaChatCall?, MegaChatSession>>(EventConstants.EVENT_SESSION_STATUS_CHANGE)
                    .removeObserver(sessionStatusObserver)

            }
        }, BackpressureStrategy.LATEST)
}