package mega.privacy.android.app.fragments.homepage.banner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.OpenLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.fragments.homepage.main.HomePageViewModel
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity
import nz.mega.sdk.MegaBanner

class BannerAdapter(private var viewModel: HomePageViewModel)
    : BaseBannerAdapter<MegaBanner>() {

    override fun bindData(
        holder: BaseViewHolder<MegaBanner>,
        data: MegaBanner?,
        position: Int,
        pageSize: Int
    ) {
        val background: SimpleDraweeView = holder.findViewById(R.id.draweeView_background)
        val title: TextView = holder.findViewById(R.id.textView_title)
        val description: TextView = holder.findViewById(R.id.textView_description)
        val image: SimpleDraweeView = holder.findViewById(R.id.draweeView_image)

        background.setImageURI(data?.imageLocation.plus(data?.backgroundImage))
        image.setImageURI(data?.imageLocation.plus(data?.image))
        title.text = data?.title
        description.text = data?.description

        (holder.findViewById(R.id.imageView_dismiss) as ImageView).setOnClickListener {
            data?.run {
                viewModel.dismissBanner(data)
            }
        }

        data?.url?.let { link ->
            holder.itemView.setOnClickListener {
                actOnActionLink(it.context, link)
            }
        }
    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.item_banner_view
    }

    private fun actOnActionLink(context: Context, link: String) {
        when (link) {
            ACHIEVEMENT -> {
                val intent = Intent(context, AchievementsActivity::class.java)
                context.startActivity(intent)
            }
            REFERRAL -> {
                val openLinkIntent = Intent(context, WebViewActivity::class.java).apply {
                    data = Uri.parse(link)
                }
                context.startActivity(openLinkIntent)
            }
        }
    }

    companion object {
        private const val ACHIEVEMENT = "https://mega.nz/achievements"
        private const val REFERRAL = "https://mega.nz/fm/refer"
    }
}