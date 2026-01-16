package mega.privacy.android.data.mapper.pitag

import mega.privacy.android.domain.entity.pitag.PitagTarget
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper for converting [PitagTarget] type into SDK pitag trigger value.
 */
internal class PitagTargetMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param [PitagTarget] type.
     * @return Char
     */
    operator fun invoke(pitagTarget: PitagTarget): Char = when (pitagTarget) {
        PitagTarget.NotApplicable -> MegaApiJava.PITAG_TARGET_NOT_APPLICABLE
        PitagTarget.CloudDrive -> MegaApiJava.PITAG_TARGET_CLOUD_DRIVE
        PitagTarget.Chat1To1 -> MegaApiJava.PITAG_TARGET_CHAT_1TO1
        PitagTarget.ChatGroup -> MegaApiJava.PITAG_TARGET_CHAT_GROUP
        PitagTarget.NoteToSelf -> MegaApiJava.PITAG_TARGET_NOTE_TO_SELF
        PitagTarget.IncomingShare -> MegaApiJava.PITAG_TARGET_INCOMING_SHARE
        PitagTarget.MultipleChats -> MegaApiJava.PITAG_TARGET_MULTIPLE_CHATS
    }
}