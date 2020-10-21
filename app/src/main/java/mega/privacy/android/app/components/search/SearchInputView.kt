package mega.privacy.android.app.components.search

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View.OnKeyListener
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText

/**
 * The EditText in the Floating Search View,
 * at where the user keys in the searching keyword
 */
class SearchInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr)  {
    private var mSearchKeyListener: OnKeyboardSearchKeyClickListener? = null
    private var mOnKeyboardDismissedListener: OnKeyboardDismissedListener? = null
    private val mOnKeyListener =
        OnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                mSearchKeyListener?.let {
                    it.onSearchKeyClicked()
                    return@OnKeyListener true
                }
            }
            false
        }


    init {
        setOnKeyListener(mOnKeyListener)
    }

    override fun onKeyPreIme(keyCode: Int, ev: KeyEvent): Boolean {
        if (ev.keyCode == KeyEvent.KEYCODE_BACK) {
            mOnKeyboardDismissedListener?.onKeyboardDismissed()
        }

        return super.onKeyPreIme(keyCode, ev)
    }

    fun setOnKeyboardDismissedListener(onKeyboardDismissedListener: OnKeyboardDismissedListener?) {
        mOnKeyboardDismissedListener = onKeyboardDismissedListener
    }

    fun setOnSearchKeyListener(searchKeyListener: OnKeyboardSearchKeyClickListener?) {
        mSearchKeyListener = searchKeyListener
    }

    interface OnKeyboardDismissedListener {
        fun onKeyboardDismissed()
    }

    interface OnKeyboardSearchKeyClickListener {
        fun onSearchKeyClicked()
    }
}