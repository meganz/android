package mega.privacy.android.data.database.converter

import androidx.room.TypeConverter
import mega.privacy.android.domain.entity.pitag.PitagTrigger

/**
 * Converter for [PitagTrigger]
 */
internal class PitagTriggerConverter {

    @TypeConverter
    fun fromPitagTrigger(pitagTrigger: PitagTrigger): String = pitagTrigger.name

    @TypeConverter
    fun toPitagTrigger(serializedData: String?): PitagTrigger =
        if (serializedData.isNullOrEmpty()) {
            PitagTrigger.NotApplicable
        } else {
            runCatching { PitagTrigger.valueOf(serializedData) }.getOrNull()
                ?: PitagTrigger.NotApplicable
        }
}