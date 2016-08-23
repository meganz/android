package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class RecentChatsFragmentLollipop extends Fragment implements RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, View.OnClickListener {

    MegaApiAndroid megaApi;

    Context context;
    ActionBar aB;
    RecyclerView listView;
    MegaContactRequestLollipopAdapter adapterList;
    GestureDetectorCompat detector;
    TextView emptyTextView;
    RecyclerView.LayoutManager mLayoutManager;

    RelativeLayout emptyLayout;
    LinearLayout buttonsEmptyLayout;
    Button inviteButton;
    Button getStartedButton;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    private ActionMode actionMode;

    private class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        public void onLongPress(MotionEvent e) {
//            View view = listView.findChildViewUnder(e.getX(), e.getY());
//            int position = listView.getChildPosition(view);
//
//            // handle long press
//            if (!adapterList.isMultipleSelect()){
//                adapterList.setMultipleSelect(true);
//
//                actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
//
//                itemClick(position);
//            }
            super.onLongPress(e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_tab, container, false);

        listView = (RecyclerView) v.findViewById(R.id.chat_recent_list_view);
        listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
        listView.setClipToPadding(false);;

        listView.addItemDecoration(new SimpleDividerItemDecoration(context));
        mLayoutManager = new MegaLinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        //Just onClick implemented
        listView.addOnItemTouchListener(this);
        listView.setItemAnimator(new DefaultItemAnimator());

        emptyLayout = (RelativeLayout) v.findViewById(R.id.empty_layout_chat_recent);
        emptyTextView = (TextView) v.findViewById(R.id.empty_text_chat_recent);

        RelativeLayout.LayoutParams emptyTextViewParams = (RelativeLayout.LayoutParams)emptyTextView.getLayoutParams();
        emptyTextViewParams.setMargins(Util.scaleWidthPx(39, outMetrics), Util.scaleHeightPx(95, outMetrics), Util.scaleWidthPx(39, outMetrics), 0);
        emptyTextView.setLayoutParams(emptyTextViewParams);

        buttonsEmptyLayout = (LinearLayout) v.findViewById(R.id.empty_buttons_layout_recent_chat);
        RelativeLayout.LayoutParams buttonsEmptyLayoutParams = (RelativeLayout.LayoutParams)buttonsEmptyLayout.getLayoutParams();
        buttonsEmptyLayoutParams.setMargins(Util.scaleWidthPx(39, outMetrics), Util.scaleHeightPx(49, outMetrics), 0, 0);
        buttonsEmptyLayout.setLayoutParams(buttonsEmptyLayoutParams);

        inviteButton = (Button) v.findViewById(R.id.invite_button);
        LinearLayout.LayoutParams inviteButtonParams = (LinearLayout.LayoutParams)inviteButton.getLayoutParams();
        inviteButtonParams.setMargins(0, Util.scaleHeightPx(4, outMetrics), 0, Util.scaleHeightPx(4, outMetrics));
        inviteButton.setLayoutParams(inviteButtonParams);
        inviteButton.setOnClickListener(this);

        getStartedButton = (Button) v.findViewById(R.id.get_started_button);
        LinearLayout.LayoutParams getStartedButtonParams = (LinearLayout.LayoutParams)getStartedButton.getLayoutParams();
        getStartedButtonParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(4, outMetrics), 0, Util.scaleHeightPx(4, outMetrics));
        getStartedButton.setLayoutParams(getStartedButtonParams);
        getStartedButton.setOnClickListener(this);

        listView.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);

        return v;
    }

    public static RecentChatsFragmentLollipop newInstance() {
        log("newInstance");
        RecentChatsFragmentLollipop fragment = new RecentChatsFragmentLollipop();
        return fragment;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        detector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public void onClick(View v) {
        log("onClick");
        switch (v.getId()) {
            case R.id.invite_button:{
                break;
            }
            case R.id.get_started_button:{
                break;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
    }

    private static void log(String log) {
        Util.log("RecentChatsFragmentLollipop", log);
    }
}
