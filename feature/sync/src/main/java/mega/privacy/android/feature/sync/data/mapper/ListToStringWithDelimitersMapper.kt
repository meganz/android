package mega.privacy.android.feature.sync.data.mapper

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

internal class ListToStringWithDelimitersMapper @Inject constructor(
    private val gson: Gson
) {

    @TypeConverter
    inline operator fun <reified T> invoke(list: List<T>?): String = gson.toJson(list)

    @TypeConverter
    inline operator fun <reified T> invoke(value: String?): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(value, type)
    }
}