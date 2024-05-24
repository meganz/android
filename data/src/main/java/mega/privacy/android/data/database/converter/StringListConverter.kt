package mega.privacy.android.data.database.converter

import androidx.room.TypeConverter

/**
 * Converter for a list of strings to Gson
 */
internal class StringListConverter {

    /**
     * Convert a [List] of [String] to [String]
     */
    @TypeConverter
    fun restoreList(listOfString: String?): List<String>? = listOfString?.split(",")

    /**
     * Convert a [List] of [String] to [String]
     */
    @TypeConverter
    fun saveList(listOfString: List<String>?): String? = listOfString?.joinToString(separator = ",")
}