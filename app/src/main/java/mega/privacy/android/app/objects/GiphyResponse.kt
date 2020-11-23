package mega.privacy.android.app.objects

import com.google.gson.annotations.SerializedName

class GiphyResponse {
    @SerializedName("data")
    var data = ArrayList<Data>()
}

class Data {
    @SerializedName("id")
    var id: String? = null

    @SerializedName("title")
    var title: String? = null

    @SerializedName("images")
    var images: Images? = null
}

class Images {
    @SerializedName("fixed_height")
    var fixedHeight: ImageAtributes? = null
}

class ImageAtributes {
    @SerializedName("height")
    var height: Int = 0

    @SerializedName("width")
    var width: Int = 0

    @SerializedName("mp4_size")
    var mp4Size: Long = 0

    @SerializedName("mp4")
    var mp4: String? = null

    @SerializedName("webp_size")
    var webpSize: Long = 0

    @SerializedName("webp")
    var webp: String? = null
}