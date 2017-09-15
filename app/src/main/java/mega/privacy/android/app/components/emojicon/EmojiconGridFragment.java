package mega.privacy.android.app.components.emojicon;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.emojicon.emoji.Emojicon;
import mega.privacy.android.app.components.emojicon.emoji.People;
import mega.privacy.android.app.components.emojicon.EmojiconRecents;


public class EmojiconGridFragment extends Fragment implements AdapterView.OnItemClickListener {
    private OnEmojiconClickedListener mOnEmojiconClickedListener;
    private EmojiconRecents mRecents;
    private Emojicon[] mEmojicons;
    private
    @Emojicon.Type
    int mEmojiconType;
    private boolean mUseSystemDefault = false;

    private static final String ARG_USE_SYSTEM_DEFAULTS = "useSystemDefaults";
    private static final String ARG_EMOJICONS = "emojicons";
    private static final String ARG_EMOJICON_TYPE = "emojiconType";

    protected static EmojiconGridFragment newInstance(Emojicon[] emojicons, EmojiconRecents recents) {
        return newInstance(Emojicon.TYPE_UNDEFINED, emojicons, recents, false);
    }

    protected static EmojiconGridFragment newInstance(
            @Emojicon.Type int type, EmojiconRecents recents, boolean useSystemDefault) {
        return newInstance(type, null, recents, useSystemDefault);
    }

    protected static EmojiconGridFragment newInstance(
            @Emojicon.Type int type, Emojicon[] emojicons, EmojiconRecents recents, boolean useSystemDefault) {
        EmojiconGridFragment emojiGridFragment = new EmojiconGridFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_EMOJICON_TYPE, type);
        args.putParcelableArray(ARG_EMOJICONS, emojicons);
        args.putBoolean(ARG_USE_SYSTEM_DEFAULTS, useSystemDefault);
        emojiGridFragment.setArguments(args);
        emojiGridFragment.setRecents(recents);
        return emojiGridFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.emojicon_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        GridView gridView = (GridView) view.findViewById(R.id.Emoji_GridView);
        Bundle bundle = getArguments();
        if (bundle == null) {
            mEmojiconType = Emojicon.TYPE_UNDEFINED;
            mEmojicons = People.DATA;
            mUseSystemDefault = false;
        } else {
            //noinspection WrongConstant
            mEmojiconType = bundle.getInt(ARG_EMOJICON_TYPE);
            if (mEmojiconType == Emojicon.TYPE_UNDEFINED) {
                Parcelable[] parcels = bundle.getParcelableArray(ARG_EMOJICONS);
                mEmojicons = new Emojicon[parcels.length];
                for (int i = 0; i < parcels.length; i++) {
                    mEmojicons[i] = (Emojicon) parcels[i];
                }
            } else {
                mEmojicons = Emojicon.getEmojicons(mEmojiconType);
            }
            mUseSystemDefault = bundle.getBoolean(ARG_USE_SYSTEM_DEFAULTS);
        }
        gridView.setAdapter(new EmojiconAdapter(view.getContext(), mEmojicons, mUseSystemDefault));
        gridView.setOnItemClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray(ARG_EMOJICONS, mEmojicons);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEmojiconClickedListener) {
            mOnEmojiconClickedListener = (OnEmojiconClickedListener) context;
        } else if (getParentFragment() instanceof OnEmojiconClickedListener) {
            mOnEmojiconClickedListener = (OnEmojiconClickedListener) getParentFragment();
        } else {
            throw new IllegalArgumentException(context + " must implement interface " + OnEmojiconClickedListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        mOnEmojiconClickedListener = null;
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mOnEmojiconClickedListener != null) {
            mOnEmojiconClickedListener.onEmojiconClicked((Emojicon) parent.getItemAtPosition(position));
        }
        if (mRecents != null) {
            mRecents.addRecentEmoji(view.getContext(), ((Emojicon) parent.getItemAtPosition(position)));
        }
    }

    private void setRecents(EmojiconRecents recents) {
        mRecents = recents;
    }

    public interface OnEmojiconClickedListener {
        void onEmojiconClicked(Emojicon emojicon);
    }
}
