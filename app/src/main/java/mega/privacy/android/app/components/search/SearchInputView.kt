package mega.privacy.android.app.components.search

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View.OnKeyListener
import androidx.appcompat.widget.AppCompatEditText

/**
 * The EditText in the Floating Search View,
 * at where the user keys in the searching keyword
 */
class SearchInputView : AppCompatEditText {
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

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
            : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
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