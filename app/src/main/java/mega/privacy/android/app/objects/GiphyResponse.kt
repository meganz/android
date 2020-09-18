package mega.privacy.android.app.objects

import com.google.gson.annotations.SerializedName

class GiphyResponse {
    @SerializedName("data")
    var data = ArrayList<Data>()

    @SerializedName("pagination")
    var pagination: Pagination? = null

    @SerializedName("meta")
    var meta: Meta? = null
}