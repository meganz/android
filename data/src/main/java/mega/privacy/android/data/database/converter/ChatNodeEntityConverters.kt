package mega.privacy.android.data.database.converter

import androidx.room.TypeConverter
import mega.privacy.android.data.mapper.getFileTypeInfoForExtension
import mega.privacy.android.data.mapper.toDuration
import mega.privacy.android.domain.entity.FileTypeInfo


/**
 * Converters for the chat node entity.
 */
class ChatNodeEntityConverters {


    /**
     * Convert a [FileTypeInfo] to [String]
     */
    @TypeConverter
    fun convertFromFileTypeInfo(fileTypeInfo: FileTypeInfo): String {
        val duration = toDuration(fileTypeInfo)?.inWholeSeconds ?: 0
        return listOf(
            fileTypeInfo.mimeType,
            fileTypeInfo.extension,
            duration
        ).joinToString(separator = ",")
    }

    /**
     * Convert a [String] to [FileTypeInfo]
     */
    @TypeConverter
    fun convertToFileTypeInfo(string: String): FileTypeInfo {
        val (mimeType, extension, duration) = string.split(",")
        return getFileTypeInfoForExtension(mimeType, extension, duration.toIntOrNull() ?: 0)
    }
}