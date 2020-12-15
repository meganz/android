package mega.privacy.android.app.fragments.homepage.banner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.zhpan.bannerview.BaseViewHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomePageViewModel
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity
import nz.mega.sdk.MegaBanner

class BannerViewHolder(itemView: View) : BaseViewHolder<MegaBanner>(itemView) {
    private lateinit var viewModel: HomePageViewModel

    override fun bindData(data: MegaBanner?, position: Int, pageSize: Int) {
        val background: SimpleDraweeView = findView(R.id.draweeView_background)
        val title: TextView = findView(R.id.textView_title)
        val description: TextView = findView(R.id.textView_description)
        val image: SimpleDraweeView = findView(R.id.draweeView_image)

        background.setImageURI(data?.imageLocation.plus(data?.backgroundImage))
        image.setImageURI(data?.imageLocation.plus(data?.image))
        title.text = data?.title
        description.text = data?.description
//        val link = data?.url

        (findView(R.id.imageView_dismiss) as ImageView  ).setOnClickListener {
            data?.run {
                viewModel.dismissBanner(data)
            }
        }

        itemView.setOnClickListener {
            actOnActionLink(itemView.context, ACHIEVEMENT)
        }
    }

    fun setViewModel(viewModel: HomePageViewModel) {
        this.viewModel = viewModel
    }

    private fun actOnActionLink(context: Context, link: String) {
        when (link) {
            ACHIEVEMENT -> {
                val intent = Intent(context, AchievementsActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    companion object {
        private const val ACHIEVEMENT = "https://mega.nz/achievements"
    }
}