package com.example.kotlintest

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View.OnKeyListener
import androidx.appcompat.widget.AppCompatEditText

class SearchInputView : AppCompatEditText {
    private var mSearchKeyListener: OnKeyboardSearchKeyClickListener? = null
    private var mOnKeyboardDismissedListener: OnKeyboardDismissedListener? = null
    private val mOnKeyListener =
        OnKeyListener { view, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && mSearchKeyListener != null) {
                mSearchKeyListener!!.onSearchKeyClicked()
                return@OnKeyListener true
            }
            false
        }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
        setOnKeyListener(mOnKeyListener)
    }

    override fun onKeyPreIme(keyCode: Int, ev: KeyEvent): Boolean {
        if (ev.keyCode == KeyEvent.KEYCODE_BACK && mOnKeyboardDismissedListener != null) {
            mOnKeyboardDismissedListener!!.onKeyboardDismissed()
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