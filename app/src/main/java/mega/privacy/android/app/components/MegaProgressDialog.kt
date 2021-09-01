package mega.privacy.android.app.components

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import mega.privacy.android.app.R

/**
 * A subclass of AlertDialog that can display the circle progress and message.
 * @Params: context – the parent context
 */
open class MegaProgressDialog(context: Context?) : AlertDialog(context), View.OnClickListener {

    var call: ClickCallBack? = null
    var txtMessage: TextView? = null
    var isCircular = true
    var yesButton: View? = null
    var noButton: View? = null

    constructor(context: Context?, title: String, yesCallBack: ClickCallBack)
            : this(context, title, yesCallBack, true) {
    }

    constructor(context: Context?, title: String, yesCallBack: ClickCallBack, isCircularProgress: Boolean) : this(context) {
        call = yesCallBack
        txtMessage?.text = title
        isCircular = isCircularProgress
    }

    override fun setMessage(message: CharSequence?) {
        txtMessage?.text = message
    }

    init {
        val inflater = LayoutInflater.from(context).inflate(R.layout.progress_bar, null);
        setView(inflater)
        txtMessage = inflater.findViewById(R.id.progress_msg)
    }


    override fun onClick(p0: View?) {
        call?.yesClick(this)
    }

    interface ClickCallBack {
        fun yesClick(dialog:MegaProgressDialog)
    }
}