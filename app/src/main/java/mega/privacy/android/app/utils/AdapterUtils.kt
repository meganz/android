package mega.privacy.android.app.utils

import androidx.recyclerview.widget.RecyclerView

object AdapterUtils {

    fun <VH : RecyclerView.ViewHolder> RecyclerView.Adapter<VH>.isValidPosition(position: Int): Boolean =
        position != RecyclerView.NO_POSITION && position < itemCount
}
