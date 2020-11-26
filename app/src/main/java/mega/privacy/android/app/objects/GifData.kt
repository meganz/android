package mega.privacy.android.app.objects

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GifData(
    val mp4Url: String?,
    val webpUrl: String?,
    val mp4Size: Long,
    val webpSize: Long,
    val width: Int,
    val height: Int,
    val title: String?
) : Parcelable