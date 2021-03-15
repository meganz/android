package mega.privacy.android.app.meeting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import java.io.File

@BindingAdapter("participantAvatar")
fun setMeetingParticipantAvatar(imageView: SimpleDraweeView, avatar: File?) {
    with(imageView) {
        if (isFileAvailable(avatar)) {
            setImageURI(Uri.fromFile(avatar))
        } else {
            setImageURI(null as Uri?)
        }
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("isMe", "isModerator", "participantName")
fun setMeetingParticipantName(
    textView: TextView,
    isMe: Boolean,
    isModerator: Boolean,
    name: String
) {
    if (isModerator) {
        textView.setCompoundDrawablesWithIntrinsicBounds(
            null, null, ContextCompat.getDrawable(textView.context, R.drawable.ic_moderator), null
        )
    } else {
        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
    }

    if (isMe) {
        textView.text = HtmlCompat.fromHtml(
            "$name <font color='${
                getColorHexString(textView.context, R.color.grey_600)
            }'>(${getString(R.string.bucket_word_me)})</font>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    } else {
        textView.text = name
    }
}

@BindingAdapter("isAudioOn")
fun setMeetingParticipantIsAudioOn(imageView: ImageView, isAudioOn: Boolean) {
    if (isAudioOn) {
        imageView.setImageResource(R.drawable.ic_mic_on)
        imageView.setColorFilter(
            ContextCompat.getColor(imageView.context, R.color.grey_alpha_054),
            PorterDuff.Mode.SRC_IN
        )
    } else {
        imageView.setImageResource(R.drawable.ic_mic_off_grey_red)
        imageView.colorFilter = null
    }
}

@BindingAdapter("isVideoOn")
fun setMeetingParticipantIsVideoOn(imageView: ImageView, isVideoOn: Boolean) {
    if (isVideoOn) {
        imageView.setImageResource(R.drawable.ic_video)
        imageView.setColorFilter(
            ContextCompat.getColor(imageView.context, R.color.grey_alpha_054),
            PorterDuff.Mode.SRC_IN
        )
    } else {
        imageView.setImageResource(R.drawable.ic_video_off_grey_red)
        imageView.colorFilter = null
    }
}
