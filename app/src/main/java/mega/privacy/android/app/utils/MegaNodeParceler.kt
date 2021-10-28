package mega.privacy.android.app.utils

import android.os.Parcel
import kotlinx.parcelize.Parceler
import nz.mega.sdk.MegaNode

object MegaNodeParceler : Parceler<MegaNode> {
    override fun create(parcel: Parcel): MegaNode = MegaNode.unserialize(parcel.readString())

    override fun MegaNode.write(parcel: Parcel, flags: Int) {
        parcel.writeString(serialize())
    }
}
