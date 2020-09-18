package mega.privacy.android.app.objects

import com.google.gson.annotations.SerializedName

class GiphySingleResponse {
    @SerializedName("data")
    var data: Data? = null

    @SerializedName("pagination")
    var pagination: Pagination? = null

    @SerializedName("meta")
    var meta: Meta? = null
}

class Data {
    @SerializedName("type")
    var type: String? = null

    @SerializedName("id")
    var id: String? = null

    @SerializedName("url")
    var url: String? = null

    @SerializedName("slug")
    var slug: String? = null

    @SerializedName("bitly_gif_url")
    var bitly_gif_url: String? = null

    @SerializedName("bitly_url")
    var bitly_url: String? = null

    @SerializedName("embed_url")
    var embed_url: String? = null

    @SerializedName("username")
    var username: String? = null

    @SerializedName("source")
    var source: String? = null

    @SerializedName("title")
    var title: String? = null

    @SerializedName("rating")
    var rating: String? = null

    @SerializedName("content_url")
    var content_url: String? = null

    @SerializedName("tags")
    var tags: ArrayList<String>? = null

    @SerializedName("featured_tags")
    var featured_tags: ArrayList<String>? = null

    @SerializedName("user_tags")
    var user_tags: ArrayList<String>? = null

    @SerializedName("source_tld")
    var source_tld: String? = null

    @SerializedName("source_post_url")
    var source_post_url: String? = null

    @SerializedName("is_sticker")
    var is_sticker: Int? = null

    @SerializedName("import_datetime")
    var import_datetime: String? = null

    @SerializedName("trending_datetime")
    var trending_datetime: String? = null

    @SerializedName("images")
    var images: Images? = null

    @SerializedName("user")
    var user: User? = null

    @SerializedName("analytics_response_payload")
    var analytics_response_payload: String? = null

    @SerializedName("analytics")
    var analytics: Analytic? = null
}

class Images {
    @SerializedName("original")
    var original: ImageAtributes? = null

    @SerializedName("downsized")
    var downsized: ImageAtributes? = null

    @SerializedName("downsized_large")
    var downsized_large: ImageAtributes? = null

    @SerializedName("downsized_medium")
    var downsized_medium: ImageAtributes? = null

    @SerializedName("downsized_small")
    var downsized_small: ImageAtributes? = null

    @SerializedName("downsized_still")
    var downsized_still: ImageAtributes? = null

    @SerializedName("fixed_height")
    var fixed_height: ImageAtributes? = null

    @SerializedName("fixed_height_downsampled")
    var fixed_height_downsampled: ImageAtributes? = null

    @SerializedName("fixed_height_small")
    var fixed_height_small: ImageAtributes? = null

    @SerializedName("fixed_height_small_still")
    var fixed_height_small_still: ImageAtributes? = null

    @SerializedName("fixed_height_still")
    var fixed_height_still: ImageAtributes? = null

    @SerializedName("fixed_width")
    var fixed_width: ImageAtributes? = null

    @SerializedName("fixed_width_downsampled")
    var fixed_width_downsampled: ImageAtributes? = null

    @SerializedName("fixed_width_small")
    var fixed_width_small: ImageAtributes? = null

    @SerializedName("fixed_width_small_still")
    var fixed_width_small_still: ImageAtributes? = null

    @SerializedName("fixed_width_still")
    var fixed_width_still: ImageAtributes? = null

    @SerializedName("looping")
    var looping: ImageAtributes? = null

    @SerializedName("original_still")
    var original_still: ImageAtributes? = null

    @SerializedName("original_mp4")
    var original_mp4: ImageAtributes? = null

    @SerializedName("preview")
    var preview: ImageAtributes? = null

    @SerializedName("preview_gif")
    var preview_gif: ImageAtributes? = null

    @SerializedName("preview_webp")
    var preview_webp: ImageAtributes? = null

    @SerializedName("480w_still")
    var _480w_still: ImageAtributes? = null
}

class ImageAtributes {
    @SerializedName("height")
    var height: Int? = null

    @SerializedName("width")
    var width: Int? = null

    @SerializedName("size")
    var size: Double? = null

    @SerializedName("url")
    var url: String? = null

    @SerializedName("mp4_size")
    var mp4_size: Double? = null

    @SerializedName("mp4")
    var mp4: String? = null

    @SerializedName("webp_size")
    var webp_size: Double? = null

    @SerializedName("webp")
    var webp: String? = null

    @SerializedName("frames")
    var frames: Int? = null

    @SerializedName("hash")
    var hash: String? = null
}

class User {
    @SerializedName("avatar_url")
    var avatar_url: String? = null

    @SerializedName("banner_image")
    var banner_image: String? = null

    @SerializedName("banner_url")
    var banner_url: String? = null

    @SerializedName("profile_url")
    var profile_url: String? = null

    @SerializedName("username")
    var bitly_gif_url: String? = null

    @SerializedName("display_name")
    var display_name: String? = null

    @SerializedName("is_verified")
    var is_verified: Boolean? = null
}

class Analytic {
    @SerializedName("onload")
    var onload: Url? = null

    @SerializedName("onclick")
    var onclick: Url? = null

    @SerializedName("onsent")
    var onsent: Url? = null
}

class Url {
    @SerializedName("url")
    var url: String? = null
}

class Pagination {
    @SerializedName("total_count")
    var total_count: Double? = null

    @SerializedName("count")
    var count: Double? = null

    @SerializedName("offset")
    var offset: Int? = null
}

class Meta {
    @SerializedName("status")
    var status: Int? = null

    @SerializedName("msg")
    var msg: String? = null

    @SerializedName("response_id")
    var response_id: Boolean? = null
}