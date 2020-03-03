package mega.privacy.android.app.components.tokenautocomplete;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mega.privacy.android.app.lollipop.AddContactActivityLollipop;

/**
 * https://github.com/splitwise/TokenAutoComplete
 *
 * Copyright (c) 2013, 2014 splitwise, Wouter Dullaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");  you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 * Gmail style auto complete view with easy token customization
 * override getViewForObject to provide your token view
 * <br>
 * Created by mgod on 9/12/13.
 *
 * Modified by MEGA on 8/27/16.
 *
 * @author mgod
 */
public abstract class TokenCompleteTextView<T> extends MultiAutoCompleteTextView implements TextView.OnEditorActionListener {
    //Logging
    public static final String TAG = "TokenAutoComplete";

    //When the token is deleted...
    public enum TokenDeleteStyle {
        _Parent, //...do the parent behavior, not recommended
        Clear, //...clear the underlying text
        PartialCompletion, //...return the original text used for completion
        ToString //...replace the token with toString of the token object
    }

    //When the user clicks on a token...
    public enum TokenClickStyle {
        None(false), //...do nothing, but make sure the cursor is not in the token
        Delete(false),//...delete the token
        Select(true),//...select the token. A second click will delete it.
        SelectDeselect(true);

        private boolean mIsSelectable = false;

        TokenClickStyle(final boolean selectable) {
            mIsSelectable = selectable;
        }

        public boolean isSelectable() {
            return mIsSelectable;
        }
    }

    private char[] splitChar = {',', ';'};
    private Tokenizer tokenizer;
    private T selectedObject;
    private TokenListener<T> listener;
    private TokenSpanWatcher spanWatcher;
    private TokenTextWatcher textWatcher;
    private ArrayList<T> objects;
    private List<TokenCompleteTextView<T>.TokenImageSpan> hiddenSpans;
    private TokenDeleteStyle deletionStyle = TokenDeleteStyle._Parent;
    private TokenClickStyle tokenClickStyle = TokenClickStyle.None;
    private CharSequence prefix = "";
    private boolean hintVisible = false;
    private Layout lastLayout = null;
    private boolean allowDuplicates = true;
    private boolean focusChanging = false;
    private boolean initialized = false;
    private boolean performBestGuess = true;
    private boolean savingState = false;
    private boolean shouldFocusNext = false;
    private boolean allowCollapse = true;

    private int tokenLimit = -1;

    /**
     * Add the TextChangedListeners
     */
    protected void addListeners() {
        Editable text = getText();
        if (text != null) {
            text.setSpan(spanWatcher, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            addTextChangedListener(textWatcher);
        }
    }

    /**
     * Remove the TextChangedListeners
     */
    protected void removeListeners() {
        Editable text = getText();
        if (text != null) {
            TokenSpanWatcher[] spanWatchers = text.getSpans(0, text.length(), TokenSpanWatcher.class);
            for (TokenSpanWatcher watcher : spanWatchers) {
                text.removeSpan(watcher);
            }
            removeTextChangedListener(textWatcher);
        }
    }

    /**
     * Initialise the variables and various listeners
     */
    private void init() {
        if (initialized) return;

        // Initialise variables
        setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        objects = new ArrayList<>();
        Editable text = getText();
        assert null != text;
        spanWatcher = new TokenSpanWatcher();
        textWatcher = new TokenTextWatcher();
        hiddenSpans = new ArrayList<>();

        // Initialise TextChangedListeners
        addListeners();

        setTextIsSelectable(false);
        setLongClickable(false);

        //In theory, get the soft keyboard to not supply suggestions. very unreliable < API 11
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        setHorizontallyScrolling(false);

        // Listen to IME action keys
        setOnEditorActionListener(this);

        // Initialise the textfilter (listens for the splitchars)
        setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                // Token limit check
                if (tokenLimit != -1 && objects.size() == tokenLimit) {
                    return "";
                } else if (source.length() == 1) {//Detect split characters, remove them and complete the current token instead
                    if (isSplitChar(source.charAt(0))) {
                        performCompletion();
                        return "";
                    }
                }

                //We need to not do anything when we would delete the prefix
                if (dstart < prefix.length()) {
                    //when settext is called, which should only be called during
                    //restoring, dstart and dend are 0. If not checked, it will clear out the prefix.
                    //this is why we need to return null in this if condition to preserve state.
                    if (dstart == 0 && dend == 0) {
                        return null;
                    } else if (dend <= prefix.length()) {
                        //Don't do anything
                        return prefix.subSequence(dstart, dend);
                    } else {
                        //Delete everything up to the prefix
                        return prefix.subSequence(dstart, prefix.length());
                    }
                }
                return null;
            }
        }});

        //We had _Parent style during initialization to handle an edge case in the parent
        //now we can switch to Clear, usually the best choice
        setDeletionStyle(TokenDeleteStyle.Clear);
        initialized = true;
    }

    public TokenCompleteTextView(Context context) {
        super(context);
        init();
    }

    public TokenCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TokenCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected void performFiltering(@NonNull CharSequence text, int start, int end,
                                    int keyCode) {
        if (start < prefix.length()) {
            start = prefix.length();
        }
        Filter filter = getFilter();
        if (filter != null) {
            if (hintVisible) {
                filter.filter("");
            } else {
                filter.filter(text.subSequence(start, end), this);
            }
        }
    }


    @Override
    public void setTokenizer(Tokenizer t) {
        super.setTokenizer(t);
        tokenizer = t;
    }

    /**
     * Set the action to be taken when a Token is removed
     *
     * @param dStyle The TokenDeleteStyle
     */
    public void setDeletionStyle(TokenDeleteStyle dStyle) {
        deletionStyle = dStyle;
    }

    /**
     * Set the action to be taken when a Token is clicked
     *
     * @param cStyle The TokenClickStyle
     */
    @SuppressWarnings("unused")
    public void setTokenClickStyle(TokenClickStyle cStyle) {
        tokenClickStyle = cStyle;
    }

    /**
     * Set the listener that will be notified of changes in the Tokenlist
     *
     * @param l The TokenListener
     */
    public void setTokenListener(TokenListener<T> l) {
        listener = l;
    }

    /**
     * Override if you want to prevent a token from being removed. Defaults to true.
     * @param token the token to check
     * @return false if the token should not be removed, true if it's ok to remove it.
     */
    @SuppressWarnings("unused")
    public boolean isTokenRemovable(T token) {
        return true;
    }

    /**
     * A String of text that is shown before all the tokens inside the EditText
     * (Think "To: " in an email address field. I would advise against this: use a label and a hint.
     *
     * @param p String with the hint
     */
    public void setPrefix(CharSequence p) {
        //Have to clear and set the actual text before saving the prefix to avoid the prefix filter
        prefix = "";
        Editable text = getText();
        if (text != null) {
            text.insert(0, p);
        }
        prefix = p;

        updateHint();
    }

    /**
     * Get the list of Tokens
     *
     * @return List of tokens
     */
    public List<T> getObjects() {
        return objects;
    }

    /**
     * Set a list of characters that should trigger the token creation
     * Because spaces are difficult to handle, we add '§' as an additional splitChar
     *
     * @param splitChar char[] with a characters that trigger the token creation
     */
    public void setSplitChar(char[] splitChar) {
        char[] fixed = splitChar;
        if (splitChar[0] == ' ') {
            fixed = new char[splitChar.length + 1];
            fixed[0] = '§';
            System.arraycopy(splitChar, 0, fixed, 1, splitChar.length);
        }
        this.splitChar = fixed;
        // Keep the tokenizer and splitchars in sync
        this.setTokenizer(new CharacterTokenizer(splitChar));
    }

    /**
     * Sets a single character to trigger the token creation
     *
     * @param splitChar char that triggers the token creation
     */
    @SuppressWarnings("unused")
    public void setSplitChar(char splitChar) {
        setSplitChar(new char[]{splitChar});
    }

    /**
     * Returns true if the character is currently configured as a splitChar
     *
     * @param c the char to test
     * @return boolean
     */
    private boolean isSplitChar(char c) {
        for (char split : splitChar) {
            if (c == split) return true;
        }
        return false;
    }

    AddContactActivityLollipop onTextChangeListener;

    public void setOnTextChangeListener(Object o){
        this.onTextChangeListener = (AddContactActivityLollipop) o;
    }

    /**
     * Sets whether to allow duplicate objects. If false, when the user selects
     * an object that's already in the view, the current text is just cleared.
     * <br>
     * Defaults to true. Requires that the objects implement equals() correctly.
     *
     * @param allow boolean
     */
    @SuppressWarnings("unused")
    public void allowDuplicates(boolean allow) {
        allowDuplicates = allow;
    }

    /**
     * Set whether we try to guess an entry from the autocomplete spinner or allow any text to be
     * entered
     *
     * @param guess true to enable guessing
     */
    @SuppressWarnings("unused")
    public void performBestGuess(boolean guess) {
        performBestGuess = guess;
    }

    /**
     * Set whether the view should collapse to a single line when it loses focus.
     *
     * @param allowCollapse true if it should collapse
     */
    @SuppressWarnings("unused")
    public void allowCollapse(boolean allowCollapse) {
        this.allowCollapse = allowCollapse;
    }

    /**
     * Set a number of tokens limit.
     *
     * @param tokenLimit The number of tokens permitted. -1 value disables limit.
     */
    @SuppressWarnings("unused")
    public void setTokenLimit(int tokenLimit) {
        this.tokenLimit = tokenLimit;
    }

    /**
     * A token view for the object
     *
     * @param object the object selected by the user from the list
     * @return a view to display a token in the text field for the object
     */
    abstract protected View getViewForObject(T object);

    /**
     * Provides a default completion when the user hits , and there is no item in the completion
     * list
     *
     * @param completionText the current text we are completing against
     * @return a best guess for what the user meant to complete
     */
    abstract protected T defaultObject(String completionText);

    /**
     * Correctly build accessibility string for token contents
     *
     * This seems to be a hidden API, but there doesn't seem to be another reasonable way
     * @return custom string for accessibility
     */
    @SuppressWarnings("unused")
    public CharSequence getTextForAccessibility() {
        if (getObjects().size() == 0) {
            return getText();
        }

        SpannableStringBuilder description = new SpannableStringBuilder();
        Editable text = getText();
        int selectionStart = -1;
        int selectionEnd = -1;
        int i;
        //Need to take the existing tet buffer and
        // - replace all tokens with a decent string representation of the object
        // - set the selection span to the corresponding location in the new CharSequence
        for (i = 0; i < text.length(); ++i) {
            //See if this is where we should start the selection
            int origSelectionStart = Selection.getSelectionStart(text);
            if (i == origSelectionStart) {
                selectionStart = description.length();
            }
            int origSelectionEnd = Selection.getSelectionEnd(text);
            if (i == origSelectionEnd) {
                selectionEnd = description.length();
            }

            //Replace token spans
            TokenImageSpan[] tokens = text.getSpans(i, i, TokenImageSpan.class);
            if (tokens.length > 0) {
                TokenImageSpan token = tokens[0];
                description = description.append(tokenizer.terminateToken(token.getToken().toString()));
                i = text.getSpanEnd(token);
                continue;
            }

            description = description.append(text.subSequence(i, i + 1));
        }

        int origSelectionStart = Selection.getSelectionStart(text);
        if (i == origSelectionStart) {
            selectionStart = description.length();
        }
        int origSelectionEnd = Selection.getSelectionEnd(text);
        if (i == origSelectionEnd) {
            selectionEnd = description.length();
        }

        if (selectionStart >= 0 && selectionEnd >= 0) {
            Selection.setSelection(description, selectionStart, selectionEnd);
        }

        return description;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            CharSequence text = getTextForAccessibility();
            event.setFromIndex(Selection.getSelectionStart(text));
            event.setToIndex(Selection.getSelectionEnd(text));
            event.setItemCount(text.length());
        }
    }

    private int getCorrectedTokenEnd() {
        Editable editable = getText();
        int cursorPosition = getSelectionEnd();
        return tokenizer.findTokenEnd(editable, cursorPosition);
    }

    private int getCorrectedTokenBeginning(int end) {
        int start = tokenizer.findTokenStart(getText(), end);
        if (start < prefix.length()) {
            start = prefix.length();
        }
        return start;
    }

    protected String currentCompletionText() {
        if (hintVisible) return ""; //Can't have any text if the hint is visible

        Editable editable = getText();
        int end = getCorrectedTokenEnd();
        int start = getCorrectedTokenBeginning(end);

        //Some keyboards add extra spaces when doing corrections, so
        return TextUtils.substring(editable, start, end);
    }

    protected float maxTextWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    boolean inInvalidate = false;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void api16Invalidate() {
        if (initialized && !inInvalidate) {
            inInvalidate = true;
            setShadowLayer(getShadowRadius(), getShadowDx(), getShadowDy(), getShadowColor());
            inInvalidate = false;
        }
    }

    @Override
    public void invalidate() {
        //Need to force the TextView private mEditor variable to reset as well on API 16 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            api16Invalidate();
        }

        super.invalidate();
    }

    @Override
    public boolean enoughToFilter() {
        if (tokenizer == null || hintVisible) {
            return false;
        }

        int cursorPosition = getSelectionEnd();

        if (cursorPosition < 0) {
            return false;
        }

        int end = getCorrectedTokenEnd();
        int start = getCorrectedTokenBeginning(end);

        //Don't allow 0 length entries to filter
        return end - start >= Math.max(getThreshold(), 1);
    }

    @Override
    public void performCompletion() {
        if ((getAdapter() == null || getListSelection() == ListView.INVALID_POSITION) && enoughToFilter()) {
            Object bestGuess;
            if (getAdapter() != null && getAdapter().getCount() > 0 && performBestGuess) {
                bestGuess = getAdapter().getItem(0);
            } else {
                bestGuess = defaultObject(currentCompletionText());
            }
            replaceText(convertSelectionToString(bestGuess));
        } else {
            super.performCompletion();
        }
    }

    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        InputConnection superConn = super.onCreateInputConnection(outAttrs);
        if (superConn != null) {
            TokenInputConnection conn = new TokenInputConnection(superConn, true);
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
            outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
            return conn;
        } else {
            return null;
        }
    }

    /**
     * Create a token and hide the keyboard when the user sends the DONE IME action
     * Use IME_NEXT if you want to create a token and go to the next field
     */
    private void handleDone() {
        // Attempt to complete the current token token
        performCompletion();

        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        boolean handled = super.onKeyUp(keyCode, event);
        if (shouldFocusNext) {
            shouldFocusNext = false;
            handleDone();
        }
        return handled;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_TAB:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.hasNoModifiers()) {
                    shouldFocusNext = true;
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                handled = !canDeleteSelection(1) || deleteSelectedObject(false);
                break;
        }

        return handled || super.onKeyDown(keyCode, event);
    }

    private boolean deleteSelectedObject(boolean handled) {
        if (tokenClickStyle != null && tokenClickStyle.isSelectable()) {
            Editable text = getText();
            if (text == null) return handled;

            TokenImageSpan[] spans = text.getSpans(0, text.length(), TokenImageSpan.class);
            for (TokenImageSpan span : spans) {
                if (span.view.isSelected()) {
                    removeSpan(span);
                    handled = true;
                    break;
                }
            }
        }
        return handled;
    }

    @Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_DONE) {
            handleDone();
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        Editable text = getText();
        boolean handled = false;

        if (tokenClickStyle == TokenClickStyle.None) {
            handled = super.onTouchEvent(event);
        }

        if (isFocused() && text != null && lastLayout != null && action == MotionEvent.ACTION_UP) {

            int offset = getOffsetForPosition(event.getX(), event.getY());

            if (offset != -1) {
                TokenImageSpan[] links = text.getSpans(offset, offset, TokenImageSpan.class);

                if (links.length > 0) {
                    links[0].onClick();
                    handled = true;
                } else {
                    //We didn't click on a token, so if any are selected, we should clear that
                    clearSelections();
                }
            }
        }

        if (!handled && tokenClickStyle != TokenClickStyle.None) {
            handled = super.onTouchEvent(event);
        }
        return handled;

    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (hintVisible) {
            //Don't let users select the hint
            selStart = 0;
        }
        //Never let users select text
        selEnd = selStart;

        if (tokenClickStyle != null && tokenClickStyle.isSelectable()) {
            Editable text = getText();
            if (text != null) {
                clearSelections();
            }
        }


        if (prefix != null && (selStart < prefix.length() || selEnd < prefix.length())) {
            //Don't let users select the prefix
            setSelection(prefix.length());
        } else {
            Editable text = getText();
            if (text != null) {
                //Make sure if we are in a span, we select the spot 1 space after the span end
                TokenImageSpan[] spans = text.getSpans(selStart, selEnd, TokenImageSpan.class);
                for (TokenImageSpan span : spans) {
                    int spanEnd = text.getSpanEnd(span);
                    if (selStart <= spanEnd && text.getSpanStart(span) < selStart) {
                        if (spanEnd == text.length())
                            setSelection(spanEnd);
                        else
                            setSelection(spanEnd + 1);
                        return;
                    }
                }

            }

            super.onSelectionChanged(selStart, selEnd);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        lastLayout = getLayout(); //Used for checking text positions
    }

    /**
     * Collapse the view by removing all the tokens not on the first line. Displays a "+x" token.
     * Restores the hidden tokens when the view gains focus.
     *
     * @param hasFocus boolean indicating whether we have the focus or not.
     */
    public void performCollapse(boolean hasFocus) {
        // Pause the spanwatcher
        focusChanging = true;
        if (!hasFocus) {
            Editable text = getText();
            if (text != null && lastLayout != null) {
                // Display +x thingy if appropriate
                int lastPosition = lastLayout.getLineVisibleEnd(0);
                TokenImageSpan[] tokens = text.getSpans(0, lastPosition, TokenImageSpan.class);
                int count = objects.size() - tokens.length;

                // Make sure we don't add more than 1 CountSpan
                CountSpan[] countSpans = text.getSpans(0, lastPosition, CountSpan.class);

                if (count > 0 && countSpans.length == 0) {
                    lastPosition++;
                    CountSpan cs = new CountSpan(count, getContext(), getCurrentTextColor(),
                            (int) getTextSize(), (int) maxTextWidth());
                    text.insert(lastPosition, cs.text);

                    float newWidth = Layout.getDesiredWidth(text, 0,
                            lastPosition + cs.text.length(), lastLayout.getPaint());
                    //If the +x span will be moved off screen, move it one token in
                    if (newWidth > maxTextWidth()) {
                        text.delete(lastPosition, lastPosition + cs.text.length());

                        if (tokens.length > 0) {
                            TokenImageSpan token = tokens[tokens.length - 1];
                            lastPosition = text.getSpanStart(token);
                            cs.setCount(count + 1);
                        } else {
                            lastPosition = prefix.length();
                        }

                        text.insert(lastPosition, cs.text);
                    }

                    text.setSpan(cs, lastPosition, lastPosition + cs.text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Remove all spans behind the count span and hold them in the hiddenSpans List
                    // The generic type information is not captured in TokenImageSpan.class so we have
                    // to perform a cast for the returned spans to coerce them to the proper generic type.
                    hiddenSpans = new ArrayList<>(Arrays.asList(
                            (TokenImageSpan[]) text.getSpans(lastPosition + cs.text.length(), text.length(), TokenImageSpan.class)));
                    for (TokenImageSpan span : hiddenSpans) {
                        removeSpan(span);
                    }
                }
            }
        } else {
            final Editable text = getText();
            if (text != null) {
                CountSpan[] counts = text.getSpans(0, text.length(), CountSpan.class);
                for (CountSpan count : counts) {
                    text.delete(text.getSpanStart(count), text.getSpanEnd(count));
                    text.removeSpan(count);
                }

                // Restore the spans we have hidden
                for (TokenImageSpan span : hiddenSpans) {
                    insertSpan(span);
                }
                hiddenSpans.clear();

                if (hintVisible) {
                    setSelection(prefix.length());
                } else {
                    // Slightly delay moving the cursor to the end. Inserting spans seems to take
                    // some time. (ugly, but what can you do :( )
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setSelection(text.length());
                        }
                    }, 10);
                }

                TokenSpanWatcher[] watchers = getText().getSpans(0, getText().length(), TokenSpanWatcher.class);
                if (watchers.length == 0) {
                    //Someone removes watchers? I'm pretty sure this isn't in this code... -mgod
                    text.setSpan(spanWatcher, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        }
        // Start the spanwatcher
        focusChanging = false;
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);

        // See if the user left any unfinished tokens and finish them
        if (!hasFocus) performCompletion();

        // Collapse the view to a single line
        if (allowCollapse) performCollapse(hasFocus);
    }

    @SuppressWarnings("unchecked cast")
    @Override
    protected CharSequence convertSelectionToString(Object object) {
        selectedObject = (T) object;

        //if the token gets deleted, this text will get put in the field instead
        switch (deletionStyle) {
            case Clear:
                return "";
            case PartialCompletion:
                return currentCompletionText();
            case ToString:
                return object != null ? object.toString() : "";
            case _Parent:
            default:
                return super.convertSelectionToString(object);

        }
    }

    private SpannableStringBuilder buildSpannableForText(CharSequence text) {
        //Add a sentinel , at the beginning so the user can remove an inner token and keep auto-completing
        //This is a hack to work around the fact that the tokenizer cannot directly detect spans
        //We don't want a space as the sentinel, and splitChar[0] is guaranteed to be something non-space
        char sentinel = splitChar[0];
        return new SpannableStringBuilder(String.valueOf(sentinel) + tokenizer.terminateToken(text));
    }

    protected TokenImageSpan buildSpanForObject(T obj) {
        if (obj == null) {
            return null;
        }
        View tokenView = getViewForObject(obj);
        return new TokenImageSpan(tokenView, obj, (int) maxTextWidth());
    }

    @Override
    protected void replaceText(CharSequence text) {
        clearComposingText();

        // Don't build a token for an empty String
        if (selectedObject == null || selectedObject.toString().equals("")) return;

        SpannableStringBuilder ssb = buildSpannableForText(text);
        TokenImageSpan tokenSpan = buildSpanForObject(selectedObject);

        Editable editable = getText();
        int cursorPosition = getSelectionEnd();
        int end = cursorPosition;
        int start = cursorPosition;
        if (!hintVisible) {
            //If you force the drop down to show when the hint is visible, you can run a completion
            //on the hint. If the hint includes commas, this truncates and inserts the hint in the field
            end = getCorrectedTokenEnd();
            start = getCorrectedTokenBeginning(end);
        }

        String original = TextUtils.substring(editable, start, end);

        if (editable != null) {
            if (tokenSpan == null) {
                editable.replace(start, end, "");
            } else if (!allowDuplicates && objects.contains(tokenSpan.getToken())) {
                editable.replace(start, end, "");
            } else {
                QwertyKeyListener.markAsReplaced(editable, start, end, original);
                editable.replace(start, end, ssb);
                editable.setSpan(tokenSpan, start, start + ssb.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    @Override
    public boolean extractText(@NonNull ExtractedTextRequest request, @NonNull ExtractedText outText) {
        try {
            return super.extractText(request, outText);
        } catch (IndexOutOfBoundsException ignored) {
            Log.d(TAG, "extractText hit IndexOutOfBoundsException. This may be normal.", ignored);
            return false;
        }
    }

    /**
     * Append a token object to the object list
     *
     * @param object     the object to add to the displayed tokens
     * @param sourceText the text used if this object is deleted
     */
    public void addObject(final T object, final CharSequence sourceText) {
        post(new Runnable() {
            @Override
            public void run() {
                if (object == null) return;
                if (!allowDuplicates && objects.contains(object)) return;
                if (tokenLimit != -1 && objects.size() == tokenLimit) return;
                insertSpan(object, sourceText);
                if (getText() != null && isFocused()) setSelection(getText().length());
            }
        });
    }

    /**
     * Shorthand for addObject(object, "")
     *
     * @param object the object to add to the displayed token
     */
    public void addObject(T object) {
        addObject(object, "");
    }

    /**
     * Remove an object from the token list. Will remove duplicates or do nothing if no object
     * present in the view.
     *
     * @param object object to remove, may be null or not in the view
     */
    public void removeObject(final T object) {
        post(new Runnable() {
            @Override
            public void run() {
                //To make sure all the appropriate callbacks happen, we just want to piggyback on the
                //existing code that handles deleting spans when the text changes
                Editable text = getText();
                if (text == null) return;

                // If the object is currently hidden, remove it
                ArrayList<TokenImageSpan> toRemove = new ArrayList<>();
                for (TokenImageSpan span : hiddenSpans) {
                    if (span.getToken().equals(object)) {
                        toRemove.add(span);
                    }
                }
                for (TokenImageSpan span : toRemove) {
                    hiddenSpans.remove(span);
                    // Remove it from the state and fire the callback
                    spanWatcher.onSpanRemoved(text, span, 0, 0);
                }

                updateCountSpan();

                // If the object is currently visible, remove it
                TokenImageSpan[] spans = text.getSpans(0, text.length(), TokenImageSpan.class);
                for (TokenImageSpan span : spans) {
                    if (span.getToken().equals(object)) {
                        removeSpan(span);
                    }
                }
            }
        });
    }

    /**
     * Set the count span the current number of hidden objects
     */
    private void updateCountSpan() {
        Editable text = getText();
        CountSpan[] counts = text.getSpans(0, text.length(), CountSpan.class);
        int newCount = hiddenSpans.size();
        for (CountSpan count : counts) {
            if (newCount == 0) {
                // No more hidden Objects: remove the CountSpan
                text.delete(text.getSpanStart(count), text.getSpanEnd(count));
                text.removeSpan(count);
            } else {
                // Update the CountSpan
                count.setCount(hiddenSpans.size());
                text.setSpan(count, text.getSpanStart(count), text.getSpanEnd(count), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /**
     * Remove a span from the current EditText and fire the appropriate callback
     *
     * @param span TokenImageSpan to be removed
     */
    private void removeSpan(TokenImageSpan span) {
        Editable text = getText();
        if (text == null) return;

        //If the spanwatcher has been removed, we need to also manually trigger onSpanRemoved
        TokenSpanWatcher[] spans = text.getSpans(0, text.length(), TokenSpanWatcher.class);
        if (spans.length == 0) {
            spanWatcher.onSpanRemoved(text, span, text.getSpanStart(span), text.getSpanEnd(span));
        }

        //Add 1 to the end because we put a " " at the end of the spans when adding them
        text.delete(text.getSpanStart(span), text.getSpanEnd(span) + 1);

        if (allowCollapse && !isFocused()) {
            updateCountSpan();
        }
    }

    /**
     * Insert a new span for an Object
     *
     * @param object     Object to create a span for
     * @param sourceText CharSequence to show when the span is removed
     */
    private void insertSpan(T object, CharSequence sourceText) {
        SpannableStringBuilder ssb = buildSpannableForText(sourceText);
        TokenImageSpan tokenSpan = buildSpanForObject(object);

        Editable editable = getText();
        if (editable == null) return;

        // If we're focused, or haven't hidden any objects yet, we can try adding it
        if (!allowCollapse || isFocused() || hiddenSpans.isEmpty()) {
            int offset = editable.length();
            //There might be a hint visible...
            if (hintVisible) {
                //...so we need to put the object in in front of the hint
                offset = prefix.length();
                editable.insert(offset, ssb);
            } else {
                String completionText = currentCompletionText();
                if (completionText != null && completionText.length() > 0) {
                    // The user has entered some text that has not yet been tokenized.
                    // Find the beginning of this text and insert the new token there.
                    offset = TextUtils.indexOf(editable, completionText);
                }
                editable.insert(offset, ssb);
            }
            editable.setSpan(tokenSpan, offset, offset + ssb.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // If we're not focused: collapse the view if necessary
            if (!isFocused() && allowCollapse) performCollapse(false);

            //In some cases, particularly the 1 to nth objects when not focused and restoring
            //onSpanAdded doesn't get called
            if (!objects.contains(object)) {
                spanWatcher.onSpanAdded(editable, tokenSpan, 0, 0);
            }
        } else {
            hiddenSpans.add(tokenSpan);
            //Need to manually call onSpanAdded here as we're not putting the span on the text
            spanWatcher.onSpanAdded(editable, tokenSpan, 0, 0);
            updateCountSpan();
        }
    }

    private void insertSpan(T object) {
        String spanString;
        // The information about the original text is lost here, so other than "toString" we have no data
        if (deletionStyle == TokenDeleteStyle.ToString) {
            spanString = object != null ? object.toString() : "";
        } else {
            spanString = "";
        }

        insertSpan(object, spanString);
    }

    private void insertSpan(TokenImageSpan span) {
        insertSpan(span.getToken());
    }

    /**
     * Remove all objects from the token list.
     * We're handling this separately because removeObject doesn't always reliably trigger
     * onSpanRemoved when called too fast.
     * If removeObject is working for you, you probably shouldn't be using this.
     */
    @SuppressWarnings("unused")
    public void clear() {
        post(new Runnable() {
            @Override
            public void run() {
                // If there's no text, we're already empty
                Editable text = getText();
                if (text == null) return;

                // Get all spans in the EditText and remove them
                TokenImageSpan[] spans = text.getSpans(0, text.length(), TokenImageSpan.class);
                for (TokenImageSpan span : spans) {
                    removeSpan(span);

                    // Make sure the callback gets called
                    spanWatcher.onSpanRemoved(text, span, text.getSpanStart(span), text.getSpanEnd(span));
                }
            }
        });
    }

    private void updateHint() {
        Editable text = getText();
        CharSequence hintText = getHint();
        if (text == null || hintText == null) {
            return;
        }

        //Show hint if we need to
        if (prefix.length() > 0) {
            HintSpan[] hints = text.getSpans(0, text.length(), HintSpan.class);
            HintSpan hint = null;
            int testLength = prefix.length();
            if (hints.length > 0) {
                hint = hints[0];
                testLength += text.getSpanEnd(hint) - text.getSpanStart(hint);
            }

            if (text.length() == testLength) {
                hintVisible = true;

                if (hint != null) {
                    return;//hint already visible
                }

                //We need to display the hint manually
                Typeface tf = getTypeface();
                int style = Typeface.NORMAL;
                if (tf != null) {
                    style = tf.getStyle();
                }
                ColorStateList colors = getHintTextColors();

                HintSpan hintSpan = new HintSpan(null, style, (int) getTextSize(), colors, colors);
                text.insert(prefix.length(), hintText);
                text.setSpan(hintSpan, prefix.length(), prefix.length() + getHint().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                setSelection(prefix.length());
            } else {
                if (hint == null) {
                    return; //hint already removed
                }

                //Remove the hint. There should only ever be one
                int sStart = text.getSpanStart(hint);
                int sEnd = text.getSpanEnd(hint);

                text.removeSpan(hint);
                text.replace(sStart, sEnd, "");

                hintVisible = false;
            }
        }
    }

    private void clearSelections() {
        if (tokenClickStyle == null || !tokenClickStyle.isSelectable()) return;

        Editable text = getText();
        if (text == null) return;

        TokenImageSpan[] tokens = text.getSpans(0, text.length(), TokenImageSpan.class);
        for (TokenImageSpan token : tokens) {
            token.view.setSelected(false);
        }
        invalidate();
    }

    protected class TokenImageSpan extends ViewSpan implements NoCopySpan {
        private T token;

        public TokenImageSpan(View d, T token, int maxWidth) {
            super(d, maxWidth);
            this.token = token;
        }

        public T getToken() {
            return this.token;
        }

        public void onClick() {
            Editable text = getText();
            if (text == null) return;

            switch (tokenClickStyle) {
                case Select:
                case SelectDeselect:

                    if (!view.isSelected()) {
                        clearSelections();
                        view.setSelected(true);
                        break;
                    }

                    if (tokenClickStyle == TokenClickStyle.SelectDeselect || !isTokenRemovable(token)) {
                        view.setSelected(false);
                        invalidate();
                        break;
                    }
                    //If the view is already selected, we want to delete it
                case Delete:
                    if (isTokenRemovable(token)) {
                        removeSpan(this);
                    }
                    break;
                case None:
                default:
                    if (getSelectionStart() != text.getSpanEnd(this) + 1) {
                        //Make sure the selection is not in the middle of the span
                        setSelection(text.getSpanEnd(this) + 1);
                    }
            }
        }
    }

    public interface TokenListener<T> {
        void onTokenAdded(T token);

        void onTokenRemoved(T token);
    }

    private class TokenSpanWatcher implements SpanWatcher {

        @SuppressWarnings("unchecked cast")
        @Override
        public void onSpanAdded(Spannable text, Object what, int start, int end) {
            if (what instanceof TokenCompleteTextView<?>.TokenImageSpan && !savingState && !focusChanging) {
                TokenImageSpan token = (TokenImageSpan) what;
                objects.add(token.getToken());

                if (listener != null)
                    listener.onTokenAdded(token.getToken());
            }
        }

        @SuppressWarnings("unchecked cast")
        @Override
        public void onSpanRemoved(Spannable text, Object what, int start, int end) {
            if (what instanceof TokenCompleteTextView<?>.TokenImageSpan && !savingState && !focusChanging) {
                TokenImageSpan token = (TokenImageSpan) what;
                if (objects.contains(token.getToken())) {
                    objects.remove(token.getToken());
                }

                if (listener != null)
                    listener.onTokenRemoved(token.getToken());
            }
        }

        @Override
        public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
        }
    }

    /**
     * deletes tokens if you delete the space in front of them
     * without this, you get the auto-complete dropdown a character early
     */
    private class TokenTextWatcher implements TextWatcher {
        ArrayList<TokenImageSpan> spansToRemove = new ArrayList<>();

        protected void removeToken(TokenImageSpan token, Editable text) {
            text.removeSpan(token);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // count > 0 means something will be deleted
            if (count > 0 && getText() != null) {
                Editable text = getText();
                int end = start + count;

                //If we're deleting a space, we want spans from 1 character before this start
                if (text.charAt(start) == ' ') {
                    start -= 1;
                }

                TokenImageSpan[] spans = text.getSpans(start, end, TokenImageSpan.class);

                //NOTE: I'm not completely sure this won't cause problems if we get stuck in a text changed loop
                //but it appears to work fine. Spans will stop getting removed if this breaks.
                ArrayList<TokenImageSpan> spansToRemove = new ArrayList<>();
                for (TokenImageSpan token : spans) {
                    if (text.getSpanStart(token) < end && start < text.getSpanEnd(token)) {
                        spansToRemove.add(token);
                    }
                }
                this.spansToRemove = spansToRemove;
            }
        }

        @Override
        public void afterTextChanged(Editable text) {
            ArrayList<TokenImageSpan> spansCopy = new ArrayList<>(spansToRemove);
            spansToRemove.clear();
            for (TokenImageSpan token : spansCopy) {
                int spanStart = text.getSpanStart(token);
                int spanEnd = text.getSpanEnd(token);

                removeToken(token, text);

                //The end of the span is the character index after it
                spanEnd--;

                //Delete any extra split chars
                if (spanEnd >= 0 && isSplitChar(text.charAt(spanEnd))) {
                    text.delete(spanEnd, spanEnd + 1);
                }

                if (spanStart >= 0 && isSplitChar(text.charAt(spanStart))) {
                    text.delete(spanStart, spanStart + 1);
                }
            }

            clearSelections();
            updateHint();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (onTextChangeListener != null){
                onTextChangeListener.onTextChanged(s, start, before, count);
            }
        }
    }

    protected ArrayList<Serializable> getSerializableObjects() {
        ArrayList<Serializable> serializables = new ArrayList<>();
        for (Object obj : getObjects()) {
            if (obj instanceof Serializable) {
                serializables.add((Serializable) obj);
            } else {
                Log.e(TAG, "Unable to save '" + obj + "'");
            }
        }
        if (serializables.size() != objects.size()) {
            String message = "You should make your objects Serializable or override\n" +
                    "getSerializableObjects and convertSerializableArrayToObjectArray";
            Log.e(TAG, message);
        }

        return serializables;
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<T> convertSerializableArrayToObjectArray(ArrayList<Serializable> s) {
        return (ArrayList<T>) (ArrayList) s;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        ArrayList<Serializable> baseObjects = getSerializableObjects();

        //We don't want to save the listeners as part of the parent
        //onSaveInstanceState, so remove them first
        removeListeners();

        //ARGH! Apparently, saving the parent state on 2.3 mutates the spannable
        //prevent this mutation from triggering add or removes of token objects ~mgod
        savingState = true;
        Parcelable superState = super.onSaveInstanceState();
        savingState = false;
        SavedState state = new SavedState(superState);

        state.prefix = prefix;
        state.allowCollapse = allowCollapse;
        state.allowDuplicates = allowDuplicates;
        state.performBestGuess = performBestGuess;
        state.tokenClickStyle = tokenClickStyle;
        state.tokenDeleteStyle = deletionStyle;
        state.baseObjects = baseObjects;
        state.splitChar = splitChar;

        //So, when the screen is locked or some other system event pauses execution,
        //onSaveInstanceState gets called, but it won't restore state later because the
        //activity is still in memory, so make sure we add the listeners again
        //They should not be restored in onInstanceState if the app is actually killed
        //as we removed them before the parent saved instance state, so our adding them in
        //onRestoreInstanceState is good.
        addListeners();

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setText(ss.prefix);
        prefix = ss.prefix;
        updateHint();
        allowCollapse = ss.allowCollapse;
        allowDuplicates = ss.allowDuplicates;
        performBestGuess = ss.performBestGuess;
        tokenClickStyle = ss.tokenClickStyle;
        deletionStyle = ss.tokenDeleteStyle;
        splitChar = ss.splitChar;

        addListeners();
        for (T obj : convertSerializableArrayToObjectArray(ss.baseObjects)) {
            addObject(obj);
        }

        // Collapse the view if necessary
        if (!isFocused() && allowCollapse) {
            post(new Runnable() {
                @Override
                public void run() {
                    //Resize the view and display the +x if appropriate
                    performCollapse(isFocused());
                }
            });
        }
    }

    /**
     * Handle saving the token state
     */
    private static class SavedState extends BaseSavedState {
        CharSequence prefix;
        boolean allowCollapse;
        boolean allowDuplicates;
        boolean performBestGuess;
        TokenClickStyle tokenClickStyle;
        TokenDeleteStyle tokenDeleteStyle;
        ArrayList<Serializable> baseObjects;
        char[] splitChar;

        @SuppressWarnings("unchecked")
        SavedState(Parcel in) {
            super(in);
            prefix = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            allowCollapse = in.readInt() != 0;
            allowDuplicates = in.readInt() != 0;
            performBestGuess = in.readInt() != 0;
            tokenClickStyle = TokenClickStyle.values()[in.readInt()];
            tokenDeleteStyle = TokenDeleteStyle.values()[in.readInt()];
            baseObjects = (ArrayList<Serializable>) in.readSerializable();
            splitChar = in.createCharArray();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            TextUtils.writeToParcel(prefix, out, 0);
            out.writeInt(allowCollapse ? 1 : 0);
            out.writeInt(allowDuplicates ? 1 : 0);
            out.writeInt(performBestGuess ? 1 : 0);
            out.writeInt(tokenClickStyle.ordinal());
            out.writeInt(tokenDeleteStyle.ordinal());
            out.writeSerializable(baseObjects);
            out.writeCharArray(splitChar);
        }

        @Override
        public String toString() {
            String str = "TokenCompleteTextView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " tokens=" + baseObjects;
            return str + "}";
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Checks if selection can be deleted. This method is called from TokenInputConnection .
     * @param beforeLength the number of characters before the current selection end to check
     * @return true if there are no non-deletable pieces of the section
     */
    @SuppressWarnings("unused")
    public boolean canDeleteSelection(int beforeLength) {
        if (objects.size() < 1) return true;

        // if beforeLength is 1, we either have no selection or the call is coming from OnKey Event.
        // In these scenarios, getSelectionStart() will return the correct value.

        int endSelection = getSelectionEnd();
        int startSelection = beforeLength == 1 ? getSelectionStart() : endSelection - beforeLength;

        Editable text = getText();
        TokenImageSpan[] spans = text.getSpans(0, text.length(), TokenImageSpan.class);

        // Iterate over all tokens and allow the deletion
        // if there are no tokens not removable in the selection
        for (TokenImageSpan span : spans) {
            int startTokenSelection = text.getSpanStart(span);
            int endTokenSelection = text.getSpanEnd(span);

            // moving on, no need to check this token
            if (isTokenRemovable(span.token)) continue;

            if (startSelection == endSelection) {
                // Delete single
                if (endTokenSelection + 1 == endSelection) {
                    return false;
                }
            } else {
                // Delete range
                // Don't delete if a non removable token is in range
                if (startSelection <= startTokenSelection
                        && endTokenSelection + 1 <= endSelection) {
                    return false;
                }
            }
        }
        return true;
    }

    private class TokenInputConnection extends InputConnectionWrapper {

        public TokenInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        // This will fire if the soft keyboard delete key is pressed.
        // The onKeyPressed method does not always do this.
        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // Shouldn't be able to delete any text with tokens that are not removable
            if (!canDeleteSelection(beforeLength)) return false;

            //Shouldn't be able to delete prefix, so don't do anything
            if (getSelectionStart() <= prefix.length()) {
                beforeLength = 0;
                return deleteSelectedObject(false) || super.deleteSurroundingText(beforeLength, afterLength);
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}