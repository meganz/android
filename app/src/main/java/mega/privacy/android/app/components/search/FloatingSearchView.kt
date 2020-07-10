package mega.privacy.android.app.components.search

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import com.example.kotlintest.SearchInputView
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util

class FloatingSearchView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private var menuBtnDrawable: DrawerArrowDrawable? = null
    private var iconClear: Drawable? = null
    private var iconBackArrow: Drawable? = null

    private var mainLayout: View? = null
    private var searchInput: SearchInputView? = null
    private var leftAction: ImageView? = null
    private var clearButton: ImageView? = null

    private var menuOpen = false
    private var isInputFocused = false
    private var skipTextChangeEvent = false
    private var oldQuery = ""

    private var queryListener: OnQueryChangeListener? = null
    private var clearSearchActionListener: OnClearSearchActionListener? = null
    private var searchListener: OnSearchListener? = null
    private var menuClickListener: OnLeftMenuClickListener? = null
    private var focusChangeListener: OnFocusChangeListener? = null

    private val drawerListener = DrawerListener()

    private val textChangedCallback: (text: CharSequence?, start: Int, count: Int, after: Int) -> Unit =
        { _, _, _, _ ->
            val textStr = searchInput?.text.toString()

            // TODO: investigate why this is called twice when pressing back on the keyboard
            if (skipTextChangeEvent || !isInputFocused) {
                skipTextChangeEvent = false
            } else {
                changeClearButton(textStr.isEmpty())
                callTextChangeCallback(textStr)
            }

            oldQuery = textStr
        }

    init {
        init()
    }

    companion object {
        private const val CLEAR_BTN_FADE_ANIM_DURATION: Long = 500
        private const val MENU_ICON_ANIM_DURATION: Long = 250
        private const val MENU_BUTTON_PROGRESS_ARROW = 1.0f
        private const val MENU_BUTTON_PROGRESS_HAMBURGER = 0.0f
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the left menu (navigation menu) is
     * clicked.
     * Note: This is only relevant when leftActionMode is
     * set to {@value #LEFT_ACTION_MODE_SHOW_HAMBURGER}
     */
    interface OnLeftMenuClickListener {
        /**
         * Called when the menu button was
         * clicked and the menu's state is now opened.
         */
        fun onMenuOpened()

        /**
         * Called when the back button was
         * clicked and the menu's state is now closed.
         */
        fun onMenuClosed()
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the clear search text action button
     * (the x to the right of the text) is clicked.
     */
    interface OnClearSearchActionListener {
        /**
         * Called when the clear search text button
         * was clicked.
         */
        fun onClearSearchClicked()
    }

    /**
     * Interface for implementing a listener to listen
     * to state changes in the query text.
     */
    interface OnQueryChangeListener {
        /**
         * Called when the query has changed. It will
         * be invoked when one or more characters in the
         * query was changed.
         *
         * @param oldQuery the previous query
         * @param newQuery the new query
         */
        fun onSearchTextChanged(oldQuery: String?, newQuery: String?)
    }

    /**
     * Interface for implementing a listener to listen
     * to when the current search has completed.
     */
    interface OnSearchListener {
        /**
         * Called when the current search has completed
         * as a result of pressing search key in the keyboard.
         * Note: This will only get called if
         * [FloatingSearchView.setShowSearchKey]} is set to true.
         * @param currentQuery the text that is currently set in the query TextView
         */
        fun onSearchAction(currentQuery: String?)
    }

    /**
     * Interface for implementing a listener to listen
     * to for focus state changes.
     */
    interface OnFocusChangeListener {
        /**
         * Called when the search bar has gained focus
         * and listeners are now active.
         */
        fun onFocus()

        /**
         * Called when the search bar has lost focus
         * and listeners are no more active.
         */
        fun onFocusCleared()
    }

    private fun init() {
        initDrawables()
        findUiElements()
        initClearButton()
        initSearchInput()
        initLeftAction()
        refreshLeftIcon()
    }

    private fun findUiElements() {
        mainLayout = View.inflate(context, R.layout.search_query_section, this)
        clearButton = findViewById<View>(R.id.clear_btn) as ImageView
        searchInput = findViewById<View?>(R.id.search_bar_text) as SearchInputView?
        leftAction = findViewById<View>(R.id.left_action) as ImageView
        clearButton = findViewById<View>(R.id.clear_btn) as ImageView
    }

    private fun initDrawables() {
        menuBtnDrawable = DrawerArrowDrawable(context)
        iconClear = Util.getWrappedDrawable(context, R.drawable.ic_clear_black_24dp)
        iconBackArrow = Util.getWrappedDrawable(context, R.drawable.ic_arrow_back_black)
    }

    private fun initClearButton() {
        clearButton?.apply {
            setImageDrawable(iconClear)
            visibility = View.INVISIBLE
            setOnClickListener {
                searchInput?.setText("")
                clearSearchActionListener?.onClearSearchClicked()
            }
        }
    }

    private fun initSearchInput() {
        searchInput?.apply {
            doOnTextChanged(textChangedCallback)

            onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (skipTextChangeEvent) {
                    skipTextChangeEvent = false
                } else if (hasFocus != isInputFocused) {
                    setSearchFocusedInternal(hasFocus)
                }
            }

            setOnSearchKeyListener(object : SearchInputView.OnKeyboardSearchKeyClickListener {
                override fun onSearchKeyClicked() {
                    searchListener?.onSearchAction(getQuery())
                    skipTextChangeEvent = true
                    setSearchFocusedInternal(false)
                }
            })
        }
    }

    private fun initLeftAction() {
        leftAction?.setOnClickListener { _ ->
            if (isInputFocused) {
                setSearchFocusedInternal(false)
            } else {
                toggleLeftMenu()
            }
        }
    }

    private fun toggleLeftMenu() {
        if (menuOpen) closeMenu(true) else openMenu(true)
    }

    private fun changeClearButton(textIsEmpty: Boolean) {
        clearButton?.apply {
            if (!textIsEmpty && visibility == View.INVISIBLE) {
                alpha = 0.0f
                visibility = View.VISIBLE
                ViewCompat.animate(this).alpha(1.0f)
                    .setDuration(CLEAR_BTN_FADE_ANIM_DURATION).start()
            } else if (textIsEmpty) {
                visibility = View.INVISIBLE
            }
        }
    }

    private fun callTextChangeCallback(text: String) {
        if (!isInputFocused || oldQuery == text) return
        queryListener?.onSearchTextChanged(oldQuery, text)
    }

    private fun setSearchFocusedInternal(focused: Boolean) {
        isInputFocused = focused
        searchInput?.apply {
            setText("")
        }

        if (focused) {
            searchInput?.requestFocus()
            changeMenuDrawable(withAnim = true, isOpen = true)
            Util.showKeyboardDelayed(searchInput)
            if (menuOpen) closeMenu(false)
            searchInput?.apply {
                isLongClickable = true
                clearButton?.visibility = if (text!!.isEmpty()) View.INVISIBLE else View.VISIBLE
            }
            focusChangeListener?.onFocus()
        } else {
            searchInput?.clearFocus()
            changeMenuDrawable(withAnim = true, isOpen = false)
            clearButton?.visibility = View.GONE
            getHostActivity()?.let { Util.hideKeyboard(it) }
            searchInput?.isLongClickable = false
            focusChangeListener?.onFocusCleared()
        }
    }

    /**
     * Returns the current query text.
     * @return the current query
     */
    private fun getQuery(): String? {
        return oldQuery
    }

    private fun changeMenuDrawable(
        withAnim: Boolean, isOpen: Boolean
    ) {
        val startValue = if (isOpen) MENU_BUTTON_PROGRESS_HAMBURGER else MENU_BUTTON_PROGRESS_ARROW
        val endValue = if (isOpen) MENU_BUTTON_PROGRESS_ARROW else MENU_BUTTON_PROGRESS_HAMBURGER

        if (withAnim) {
            val anim = ValueAnimator.ofFloat(startValue, endValue)
            anim.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                menuBtnDrawable?.progress = value
            }
            anim.duration = MENU_ICON_ANIM_DURATION
            anim.start()
        } else {
            menuBtnDrawable?.progress = endValue
        }
    }

    /**
     * Sets the listener that will be called when the
     * left/start menu (or navigation menu) is clicked.
     * Note that this is different from the overflow menu
     * that has a separate listener.
     * @param listener
     */
    public fun setOnLeftMenuClickListener(listener: OnLeftMenuClickListener?) {
        menuClickListener = listener
    }

    private fun refreshLeftIcon() {
        leftAction?.setImageDrawable(menuBtnDrawable)
        menuBtnDrawable?.progress = MENU_BUTTON_PROGRESS_HAMBURGER
    }

    public fun attachNavigationDrawerToMenuButton(@NonNull drawerLayout: DrawerLayout) {
        drawerLayout.addDrawerListener(drawerListener)
        setOnLeftMenuClickListener(NavDrawerLeftMenuClickListener(drawerLayout))
    }

    private class NavDrawerLeftMenuClickListener(var drawerLayout: DrawerLayout) :
            OnLeftMenuClickListener {
        override fun onMenuOpened() {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        override fun onMenuClosed() {
            //do nothing
        }
    }

    /**
     * Enables clients to directly manipulate
     * the menu icon's progress.
     * Useful for custom animation/behaviors.
     * @param progress the desired progress of the menu
     * icon's rotation: 0.0 == hamburger
     * shape, 1.0 == back arrow shape
     */
    fun setMenuIconProgress(progress: Float) {
        menuBtnDrawable?.progress = progress
        if (progress == MENU_BUTTON_PROGRESS_HAMBURGER) {
            closeMenu(false)
        } else if (progress == MENU_BUTTON_PROGRESS_ARROW) {
            openMenu(false)
        }
    }

    inner class DrawerListener : DrawerLayout.DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            setMenuIconProgress(slideOffset)
        }

        override fun onDrawerOpened(drawerView: View) {}
        override fun onDrawerClosed(drawerView: View) {}
        override fun onDrawerStateChanged(newState: Int) {}
    }

    /**
     * Mimics a menu click that opens the menu. Useful for navigation
     * drawers when they open as a result of dragging.
     */
    fun openMenu(withAnim: Boolean) {
        menuOpen = true
        changeMenuDrawable(withAnim, true)
        menuClickListener?.onMenuOpened()
    }

    /**
     * Mimics a menu click that closes. Useful when fo navigation
     * drawers when they close as a result of selecting and item.
     *
     * @param withAnim true, will close the menu button with
     * the  Material animation
     */
    fun closeMenu(withAnim: Boolean) {
        menuOpen = false
        changeMenuDrawable(withAnim, false)
        menuClickListener?.onMenuClosed()
    }

    /**
     * Sets the listener that will be called when the focus
     * of the search has changed.
     *
     * @param listener listener for search focus changes
     */
     public fun setOnFocusChangeListener(listener: OnFocusChangeListener?) {
        focusChangeListener = listener
    }

    private fun getHostActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context as Activity
            }
            context = context.baseContext
        }
        return null
    }
}