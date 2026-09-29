package mega.privacy.android.data.model.giphy

import com.google.gson.annotations.SerializedName

/**
 * Response of the Giphy search and trending endpoints.
 */
internal data class GiphyResponse(
    @SerializedName("data") val data: List<GiphyData>?,
)

/**
 * A single GIF of a [GiphyResponse].
 */
internal data class GiphyData(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("images") val images: GiphyImages?,
)

/**
 * Renditions of a [GiphyData].
 */
internal data class GiphyImages(
    @SerializedName("fixed_height") val fixedHeight: GiphyImageAttributes?,
)

/**
 * Attributes of a GIF rendition.
 */
internal data class GiphyImageAttributes(
    @SerializedName("height") val height: Int?,
    @SerializedName("width") val width: Int?,
    @SerializedName("mp4_size") val mp4Size: Long?,
    @SerializedName("mp4") val mp4: String?,
    @SerializedName("webp_size") val webpSize: Long?,
    @SerializedName("webp") val webp: String?,
)
