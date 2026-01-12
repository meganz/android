package mega.privacy.android.data.mapper.pitag

import mega.privacy.android.domain.entity.pitag.PitagTrigger
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper for converting [PitagTrigger] type into SDK pitag trigger value.
 */
internal class PitagTriggerMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param [PitagTrigger] type.
     * @return Char
     */
    operator fun invoke(pitagTrigger: PitagTrigger): Char = when (pitagTrigger) {
        PitagTrigger.NotApplicable -> MegaApiJava.PITAG_TRIGGER_NOT_APPLICABLE
        PitagTrigger.Picker -> MegaApiJava.PITAG_TRIGGER_PICKER
        PitagTrigger.DragAndDrop -> MegaApiJava.PITAG_TRIGGER_DRAG_AND_DROP
        PitagTrigger.Camera -> MegaApiJava.PITAG_TRIGGER_CAMERA
        PitagTrigger.Scanner -> MegaApiJava.PITAG_TRIGGER_SCANNER
        PitagTrigger.SyncAlgorithm -> MegaApiJava.PITAG_TRIGGER_SYNC_ALGORITHM
    }
}