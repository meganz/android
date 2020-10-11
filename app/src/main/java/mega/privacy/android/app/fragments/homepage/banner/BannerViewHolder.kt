package mega.privacy.android.app.fragments.homepage.banner

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.zhpan.bannerview.BaseViewHolder
import kotlinx.android.synthetic.main.chat_link_share_dialog.view.*
import kotlinx.android.synthetic.main.item_banner_view.view.*
import mega.privacy.android.app.R
import nz.mega.sdk.MegaBanner

class BannerViewHolder(itemView: View) : BaseViewHolder<MegaBanner>(itemView) {
    override fun bindData(data: MegaBanner?, position: Int, pageSize: Int) {
        val background: SimpleDraweeView = findView(R.id.draweeView_background)
        val title: TextView = findView(R.id.textView_title)
        val description: TextView = findView(R.id.textView_description)
        val image: SimpleDraweeView = findView(R.id.draweeView_image)

        background.setImageURI(data?.imageLocation.plus(data?.backgroundImage))
        image.setImageURI(data?.imageLocation.plus(data?.image))
        title.text = data?.title
        description.text = data?.description
    }
}