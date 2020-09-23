package mega.privacy.android.app.objects

import android.os.Parcel
import android.os.Parcelable

data class GifData(val mp4Url: String?,
                   val webpUrl: String?,
                   val mp4Size: Long?,
                   val webpSize: Long?,
                   val width: Int,
                   val height: Int,
                   val title: String?): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readValue(Long::class.java.classLoader) as? Long,
            parcel.readValue(Long::class.java.classLoader) as? Long,
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mp4Url)
        parcel.writeString(webpUrl)
        parcel.writeValue(mp4Size)
        parcel.writeValue(webpSize)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GifData> {
        override fun createFromParcel(parcel: Parcel): GifData {
            return GifData(parcel)
        }

        override fun newArray(size: Int): Array<GifData?> {
            return arrayOfNulls(size)
        }
    }

}