package mega.privacy.android.app.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import mega.privacy.android.app.R

class CategoryButton(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CategoryButton, 0, 0)

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.category_btn_view, this, true)

        (getViewById(R.id.imageView_category) as ImageView).setImageDrawable(
            typedArray.getDrawable(R.styleable.CategoryButton_icon)
        )
        (getViewById(R.id.textView_category) as TextView).text = typedArray.getText(
            R.styleable.CategoryButton_name
        )

        typedArray.recycle()
    }
}