package mega.privacy.android.app.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mega.privacy.android.app.R

class OnOffFab(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    FloatingActionButton(context, attrs, defStyleAttr) {

    var isOn: Boolean = true
        set(value) {
            field = value

            updateAppearance()
        }

    private var onIcon: Drawable?
    private val offIcon: Drawable?

    @ColorInt
    private val onIconTint: Int

    @ColorInt
    private val offIconTint: Int

    @ColorInt
    private val onBackgroundTint: Int

    @ColorInt
    private val offBackgroundTint: Int

    private var onOffCallback: ((Boolean) -> Boolean)? = null

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.OnOffFab, defStyleAttr, 0)

        isOn = a.getBoolean(R.styleable.OnOffFab_is_on, true)

        onIcon = a.getDrawable(R.styleable.OnOffFab_on_icon)
        offIcon = a.getDrawable(R.styleable.OnOffFab_off_icon)

        onIconTint = a.getColor(
            R.styleable.OnOffFab_on_icon_tint,
            ContextCompat.getColor(context, R.color.white)
        )
        offIconTint = a.getColor(
            R.styleable.OnOffFab_off_icon_tint,
            ContextCompat.getColor(context, R.color.red_600)
        )

        onBackgroundTint = a.getColor(
            R.styleable.OnOffFab_on_background_tint,
            ContextCompat.getColor(context, R.color.grey_alpha_032)
        )
        offBackgroundTint = a.getColor(
            R.styleable.OnOffFab_off_background_tint,
            ContextCompat.getColor(context, R.color.white)
        )

        a.recycle()

        updateAppearance()

        setOnClickListener {
            if (onOffCallback?.invoke(!isOn) == true) {
                isOn = !isOn
                updateAppearance()
            }
        }
    }

    fun setOnOffCallback(callback: (Boolean) -> Boolean) {
        onOffCallback = callback
    }

    fun setOnIcon(@DrawableRes icon: Int) {
        onIcon = ContextCompat.getDrawable(context, icon)
        updateAppearance()
    }

    private fun updateAppearance() {
        setImageDrawable(if (isOn) onIcon else offIcon)
        imageTintList = ColorStateList.valueOf(if (isOn) onIconTint else offIconTint)
        backgroundTintList =
            ColorStateList.valueOf(if (isOn) onBackgroundTint else offBackgroundTint)
    }
}
