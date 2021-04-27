package mega.privacy.android.app.components

import android.R.attr.titleTextColor
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_chat.view.*
import mega.privacy.android.app.R


class LeftMeetingItem : RelativeLayout {

    private lateinit var icon: ImageView
    private lateinit var title: TextView
    private lateinit var subTitle: TextView


    private val iconSrc: Drawable?
    private var titleText: String? = null
    private var subTitleText: String? = null


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        View.inflate(context, R.layout.item_left_meeting, this)

        val a: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.LeftMeetingItem)
        titleText = a.getString(R.styleable.LeftMeetingItem_title)
        subTitleText = a.getString(R.styleable.LeftMeetingItem_subTitle)

        iconSrc = a.getDrawable(R.styleable.LeftMeetingItem_item_icon)

        a.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        icon = findViewById(R.id.icon)
        title = findViewById(R.id.title)
        subTitle = findViewById(R.id.subTitle)

        icon.setImageDrawable(iconSrc)
        title.text = titleText
        subTitle.text = subTitleText

    }
}