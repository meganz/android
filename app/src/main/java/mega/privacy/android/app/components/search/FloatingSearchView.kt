package mega.privacy.android.app.components.search

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.SearchQuerySectionBinding
import mega.privacy.android.app.lollipop.megachat.BadgeDrawerArrowDrawable
import mega.privacy.android.app.utils.Util

/**
 * Floating Search View aka Persistent search view
 * Conventionally serves as a global search box (not for contextual searching)
 * situated on the top of the screen
 * The implementation refers to https://github.com/arimorty/floatingsearchview
 */
class FloatingSearchView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val binding = SearchQuerySectionBinding.inflate(LayoutInflater.from(context))

    private val leftAction = binding.leftAction
    private val searchInput = binding.searchInput
    private val clearBtn = binding.clearBtn

    // The drawable in leftActionImageView, switching between hamburger icon and the left arrow icon
    private var menuBtnDrawable: BadgeDrawerArrowDrawable? = null
    private var menuBtnShowDot = false

    // The Clear button sits on the right and only shows when inputting something
    private var iconClear: Drawable? = null

    // The left Navigation Drawer is opened or closed
    private var menuOpen = false

    private var isInputFocused = false

    // Click the ENTER key on the keyboard would also trigger the text changed callback, so ignore it
    private var skipTextChangeEvent = false

    // The searching keyword
    private var query = ""

    private var queryListener: OnQueryChangeListener? = null
    private var clearSearchActionListener: OnClearSearchActionListener? = null
    private var searchListener: OnSearchListener? = null
    private var menuClickListener: OnLeftMenuClickListener? = null
    private var focusChangeListener: OnFocusChangeListener? = null

    private val drawerListener = DrawerListener()

    init {
        addView(binding.root)

        initDrawables()
        initClearButton()
        initSearchInput()
        initLeftAction()
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the left menu (navigation menu) is
     * clicked.
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

    inner class DrawerListener : DrawerLayout.DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            setMenuIconProgress(slideOffset)
        }

        override fun onDrawerOpened(drawerView: View) {}
        override fun onDrawerClosed(drawerView: View) {}
        override fun onDrawerStateChanged(newState: Int) {}
    }

    /**
     * Sets the listener that will be called when the
     * left/start menu (or navigation menu) is clicked.
     *
     * @param listener
     */
    fun setOnLeftMenuClickListener(listener: OnLeftMenuClickListener?) {
        menuClickListener = listener
    }

    fun attachNavigationDrawerToMenuButton(@NonNull drawerLayout: DrawerLayout) {
        drawerLayout.addDrawerListener(drawerListener)
        setOnLeftMenuClickListener(NavDrawerLeftMenuClickListener(drawerLayout))
    }

    /**
     * Sets the listener that will be called when the focus
     * of the search has changed.
     *
     * @param listener listener for search focus changes
     */
    fun setOnFocusChangeListener(listener: OnFocusChangeListener?) {
        focusChangeListener = listener
    }

    fun setShowLeftDot(showLeftDot: Boolean) {
        menuBtnShowDot = showLeftDot
        val searchInputHasFocus = searchInput.hasFocus()
        menuBtnDrawable?.setBadgeEnabled(showLeftDot && !searchInputHasFocus)
    }

    fun setAvatar(avatar: Bitmap) {
        binding.avatarImage.setImageBitmap(avatar)
    }

    fun setChatStatus(visible: Boolean, icon: Int) {
        if (visible) {
            binding.chatStatusIcon.visibility = View.VISIBLE
            binding.chatStatusIcon.setImageDrawable(ContextCompat.getDrawable(context, icon))
        }
    }

    fun setAvatarClickListener(listener: OnClickListener) {
        binding.avatarImage.setOnClickListener(listener)
    }

    fun setOnSearchInputClickListener(listener: OnClickListener) {
        binding.searchInput.setOnClickListener(listener)
    }

    private fun initDrawables() {
        menuBtnDrawable = BadgeDrawerArrowDrawable(context)
        menuBtnDrawable?.backgroundColor =
            ContextCompat.getColor(context, R.color.dark_primary_color)
        menuBtnDrawable?.setBigBackgroundColor(Color.WHITE)
        menuBtnDrawable?.setShowDot(true)
        menuBtnDrawable?.setBadgeEnabled(menuBtnShowDot)
        iconClear = Util.getWrappedDrawable(context, R.drawable.ic_clear_black)
    }

    private fun initClearButton() {
        clearBtn.apply {
            setImageDrawable(iconClear)
            visibility = View.INVISIBLE
            setOnClickListener {
                searchInput.setText("")
                clearSearchActionListener?.onClearSearchClicked()
            }
        }
    }

    private fun initSearchInput() {
        val textChangedCallback: (text: CharSequence?, start: Int, count: Int, after: Int) -> Unit =
            { _, _, _, _ ->
                val textStr = searchInput.text.toString()

                if (skipTextChangeEvent || !isInputFocused) {
                    skipTextChangeEvent = false
                } else {
                    changeClearButton(textStr.isEmpty())
                    callTextChangeCallback(textStr)
                }

                query = textStr
            }

        searchInput.apply {
            doOnTextChanged(textChangedCallback)

            onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (skipTextChangeEvent) {
                    skipTextChangeEvent = false
                } else if (hasFocus != isInputFocused) {
                    setSearchFocusedInternal(hasFocus)
                }

                menuBtnDrawable?.setBadgeEnabled(menuBtnShowDot && !hasFocus)
                binding.avatarContainer.visibility = if (hasFocus) View.GONE else View.VISIBLE
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
        leftAction.setImageDrawable(menuBtnDrawable)
        menuBtnDrawable?.progress = MENU_BUTTON_PROGRESS_HAMBURGER

        leftAction.setOnClickListener { _ ->
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
        clearBtn.apply {
            if (!textIsEmpty && visibility == View.INVISIBLE) {
                alpha = 0.0f
                visibility = View.VISIBLE
                ViewCompat.animate(this).alpha(1.0f)
                    .setDuration(CLEAR_BTN_FADE_IN_ANIM_DURATION).start()
            } else if (textIsEmpty) {
                visibility = View.INVISIBLE
            }
        }
    }

    private fun callTextChangeCallback(text: String) {
        if (!isInputFocused || query == text) return
        queryListener?.onSearchTextChanged(query, text)
    }

    private fun setSearchFocusedInternal(focused: Boolean) {
        isInputFocused = focused

        if (focused) {
            searchInput.requestFocus()
            Util.showKeyboardDelayed(searchInput)
            changeMenuDrawable(withAnim = true, isOpen = true)
            if (menuOpen) closeMenu(false)
            searchInput.apply {
                isLongClickable = true
                clearBtn.visibility = if (text!!.isEmpty()) View.INVISIBLE else View.VISIBLE
            }
            focusChangeListener?.onFocus()
        } else {
            searchInput.apply {
                setText("")
            }
            getHostActivity()?.let { Util.hideKeyboard(it) }
            searchInput.clearFocus()
            changeMenuDrawable(withAnim = true, isOpen = false)
            clearBtn.visibility = View.GONE
            searchInput.isLongClickable = false
            focusChangeListener?.onFocusCleared()
        }
    }

    /**
     * Returns the current query text.
     * @return the current query
     */
    private fun getQuery(): String? {
        return query
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

    /**
     * Mimics a menu click that opens the menu. Useful for navigation
     * drawers when they open as a result of dragging.
     */
    private fun openMenu(withAnim: Boolean) {
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
    private fun closeMenu(withAnim: Boolean) {
        menuOpen = false
        changeMenuDrawable(withAnim, false)
        menuClickListener?.onMenuClosed()
    }

    private fun getHostActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }

        return null
    }

    companion object {
        private const val CLEAR_BTN_FADE_IN_ANIM_DURATION: Long = 500
        private const val MENU_ICON_ANIM_DURATION: Long = 250
        private const val MENU_BUTTON_PROGRESS_ARROW = 1.0f
        private const val MENU_BUTTON_PROGRESS_HAMBURGER = 0.0f
    }
}