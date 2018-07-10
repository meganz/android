/*
 *
 * https://github.com/rockerhieu/emojicon
 *
 * Copyright 2014 Hieu Rocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mega.privacy.android.app.components.emojicon;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.Arrays;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.emojicon.emoji.*;
import mega.privacy.android.app.components.emojicon.EmojiconRecentsGridFragment;
import mega.privacy.android.app.components.emojicon.EmojiconRecentsManager;
import mega.privacy.android.app.components.emojicon.emoji.Emojicon;


public class EmojiconsFragment extends Fragment implements ViewPager.OnPageChangeListener, EmojiconRecents {
    private OnEmojiconBackspaceClickedListener mOnEmojiconBackspaceClickedListener;
    private int mEmojiTabLastSelectedIndex = -1;
    private TabLayout mTabs;

    private ViewPager mViewPager;
    private PagerAdapter mEmojisAdapter;
    private EmojiconRecentsManager mRecentsManager;
    private boolean mUseSystemDefault = false;

    private static final String USE_SYSTEM_DEFAULT_KEY = "useSystemDefaults";

    public static EmojiconsFragment newInstance(boolean useSystemDefault) {
        EmojiconsFragment fragment = new EmojiconsFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(USE_SYSTEM_DEFAULT_KEY, useSystemDefault);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.emojicons, container, false);
        view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));

        mViewPager = (ViewPager) view.findViewById(R.id.emojis_pager);
        mTabs =  (TabLayout) view.findViewById(R.id.sliding_tabs);
        mViewPager.setOnPageChangeListener(this);

        EmojiconRecents recents = this;
        mEmojisAdapter = new EmojiconGridFragmentPagerAdapter(getFragmentManager(), Arrays.asList(
                EmojiconRecentsGridFragment.newInstance(mUseSystemDefault),
                EmojiconGridFragment.newInstance(Emojicon.TYPE_PEOPLE, recents, mUseSystemDefault),
                EmojiconGridFragment.newInstance(Emojicon.TYPE_NATURE, recents, mUseSystemDefault),
                EmojiconGridFragment.newInstance(Emojicon.TYPE_OBJECTS, recents, mUseSystemDefault),
                EmojiconGridFragment.newInstance(Emojicon.TYPE_PLACES, recents, mUseSystemDefault),
                EmojiconGridFragment.newInstance(Emojicon.TYPE_SYMBOLS, recents, mUseSystemDefault)
        ));
        mViewPager.setAdapter(mEmojisAdapter);

        mTabs.setupWithViewPager(mViewPager);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
           mTabs.getTabAt(0).setCustomView(R.layout.ic_emoji_recent);
            mTabs.getTabAt(1).setCustomView(R.layout.ic_emoji_people);
            mTabs.getTabAt(2).setCustomView(R.layout.ic_emoji_nature);
            mTabs.getTabAt(3).setCustomView(R.layout.ic_emoji_objects);
            mTabs.getTabAt(4).setCustomView(R.layout.ic_emoji_places);
            mTabs.getTabAt(5).setCustomView(R.layout.ic_emoji_symbols);
        }else{
            mTabs.getTabAt(0).setIcon(R.drawable.ic_emoji_recent_light);
            mTabs.getTabAt(1).setIcon(R.drawable.ic_emoji_people_light);
            mTabs.getTabAt(2).setIcon(R.drawable.ic_emoji_nature_light);
            mTabs.getTabAt(3).setIcon(R.drawable.ic_emoji_objects_light);
            mTabs.getTabAt(4).setIcon(R.drawable.ic_emoji_places_light);
            mTabs.getTabAt(5).setIcon(R.drawable.ic_emoji_symbols_light);


        }



        view.findViewById(R.id.emojis_backspace).setOnTouchListener(new RepeatListener(1000, 50, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnEmojiconBackspaceClickedListener != null) {
                    mOnEmojiconBackspaceClickedListener.onEmojiconBackspaceClicked(v);
                }
            }
        }));

        // get last selected page
        mRecentsManager = EmojiconRecentsManager.getInstance(view.getContext());
        int page = mRecentsManager.getRecentPage();
        // last page was recents, check if there are recents to use
        // if none was found, go to page 1
        if (page == 0 && mRecentsManager.size() == 0) {
            page = 1;
        }

        if (page == 0) {
            onPageSelected(page);
        } else {
            mViewPager.setCurrentItem(page, false);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnEmojiconBackspaceClickedListener) {
            mOnEmojiconBackspaceClickedListener = (OnEmojiconBackspaceClickedListener) getActivity();
        } else if (getParentFragment() instanceof OnEmojiconBackspaceClickedListener) {
            mOnEmojiconBackspaceClickedListener = (OnEmojiconBackspaceClickedListener) getParentFragment();
        } else {
            throw new IllegalArgumentException(context + " must implement interface " + OnEmojiconBackspaceClickedListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        mOnEmojiconBackspaceClickedListener = null;
        super.onDetach();
    }

    public static void input(EditText editText, Emojicon emojicon) {
        if (editText == null || emojicon == null) {
            return;
        }

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start < 0) {
            editText.append(emojicon.getEmoji());
        } else {
            editText.getText().replace(Math.min(start, end), Math.max(start, end), emojicon.getEmoji(), 0, emojicon.getEmoji().length());
        }
    }

    public void addRecentEmoji(Context context, Emojicon emojicon) {
        EmojiconRecentsGridFragment fragment = (EmojiconRecentsGridFragment) mEmojisAdapter.instantiateItem(mViewPager, 0);
        fragment.addRecentEmoji(context, emojicon);
    }

    public static void backspace(EditText editText) {
        KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        editText.dispatchKeyEvent(event);
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
    }

    @Override
    public void onPageSelected(int i) {
        if (mEmojiTabLastSelectedIndex == i) {
            return;
        }
        switch (i) {

            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                mEmojiTabLastSelectedIndex = i;
                mRecentsManager.setRecentPage(i);
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    private static class EmojiconGridFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private List<EmojiconGridFragment> fragments;

        public EmojiconGridFragmentPagerAdapter(FragmentManager fm, List<EmojiconGridFragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }

    public static class RepeatListener implements View.OnTouchListener {

        private Handler handler = new Handler();

        private int initialInterval;
        private final int normalInterval;
        private final View.OnClickListener clickListener;

        private Runnable handlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (downView == null) {
                    return;
                }
                handler.removeCallbacksAndMessages(downView);
                handler.postAtTime(this, downView, SystemClock.uptimeMillis() + normalInterval);
                clickListener.onClick(downView);
            }
        };

        private View downView;

        /**
         * @param initialInterval The interval before first click event
         * @param normalInterval  The interval before second and subsequent click
         *                        events
         * @param clickListener   The OnClickListener, that will be called
         *                        periodically
         */
        public RepeatListener(int initialInterval, int normalInterval, View.OnClickListener clickListener) {
            if (clickListener == null)
                throw new IllegalArgumentException("null runnable");
            if (initialInterval < 0 || normalInterval < 0)
                throw new IllegalArgumentException("negative interval");

            this.initialInterval = initialInterval;
            this.normalInterval = normalInterval;
            this.clickListener = clickListener;
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downView = view;
                    handler.removeCallbacks(handlerRunnable);
                    handler.postAtTime(handlerRunnable, downView, SystemClock.uptimeMillis() + initialInterval);
                    clickListener.onClick(view);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    handler.removeCallbacksAndMessages(downView);
                    downView = null;
                    return true;
            }
            return false;
        }
    }

    public interface OnEmojiconBackspaceClickedListener {
        void onEmojiconBackspaceClicked(View v);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUseSystemDefault = getArguments().getBoolean(USE_SYSTEM_DEFAULT_KEY);
        } else {
            mUseSystemDefault = false;
        }
    }
}
