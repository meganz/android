package mega.privacy.android.feature.sync.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

internal class ListToStringWithDelimitersMapper @Inject constructor(
    private val gson: Gson
) {

    inline operator fun <reified T> invoke(list: List<T>?): String = gson.toJson(list)

    inline operator fun <reified T> invoke(value: String?): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(value, type)
    }
}