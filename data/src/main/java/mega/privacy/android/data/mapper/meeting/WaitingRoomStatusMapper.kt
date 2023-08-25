package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.WaitingRoomStatus
import nz.mega.sdk.MegaChatWaitingRoom
import javax.inject.Inject

/**
 * Mega waiting room status mapper
 * Maps [MegaChatWaitingRoom] to [WaitingRoomStatus]
 * If any unexpected status comes [WaitingRoomStatus.Unknown] is returned
 */
internal class WaitingRoomStatusMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param status [Int] value denoting MegaChatWaitingRoom status
     * @return [WaitingRoomStatus]
     */
    operator fun invoke(status: Int): WaitingRoomStatus = when (status) {
        MegaChatWaitingRoom.MWR_ALLOWED -> WaitingRoomStatus.Allowed
        MegaChatWaitingRoom.MWR_NOT_ALLOWED -> WaitingRoomStatus.NotAllowed
        else -> WaitingRoomStatus.Unknown
    }
}