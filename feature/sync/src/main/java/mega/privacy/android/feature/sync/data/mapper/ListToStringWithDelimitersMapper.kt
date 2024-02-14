package mega.privacy.android.feature.sync.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper to convert list to json and Json to List of T
 * @property gson [Gson]
 */
@Singleton
class ListToStringWithDelimitersMapper @Inject constructor(
    val gson: Gson
) {

    /**
     * invoke to convert list to JSON using [Gson]
     * @param list
     * @return json String
     */
    inline operator fun <reified T> invoke(list: List<T>?): String = gson.toJson(list)

    /**
     * invoke to return list of T using json string
     * @param value Json String
     * @return List of T
     */
    inline operator fun <reified T> invoke(value: String?): List<T> {
        return gson.fromJson(value, object : TypeToken<ArrayList<T>>() {}.type)
    }
}