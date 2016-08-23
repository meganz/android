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
import android.widget.ImageView;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class ArchiveChatsFragmentLollipop extends Fragment implements RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

    MegaApiAndroid megaApi;

    Context context;
    ActionBar aB;
    RecyclerView listView;
    MegaContactRequestLollipopAdapter adapterList;
    GestureDetectorCompat detector;
    ImageView emptyImageView;
    TextView emptyTextView;
    RecyclerView.LayoutManager mLayoutManager;

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

        View v = inflater.inflate(R.layout.chat_archive_tab, container, false);

        listView = (RecyclerView) v.findViewById(R.id.chat_archive_list_view);
        listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
        listView.setClipToPadding(false);;

        listView.addItemDecoration(new SimpleDividerItemDecoration(context));
        mLayoutManager = new MegaLinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        //Just onClick implemented
        listView.addOnItemTouchListener(this);
        listView.setItemAnimator(new DefaultItemAnimator());

        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_chat_archive);
        emptyTextView = (TextView) v.findViewById(R.id.empty_text_chat_archive);

        emptyImageView.setImageResource(R.drawable.sent_requests_empty);
        emptyTextView.setText(R.string.archive_chat_empty);
        listView.setVisibility(View.GONE);

        emptyImageView.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.VISIBLE);

        return v;
    }

    public static ArchiveChatsFragmentLollipop newInstance() {
        log("newInstance");
        ArchiveChatsFragmentLollipop fragment = new ArchiveChatsFragmentLollipop();
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
        Util.log("ArchiveChatsFragmentLollipop", log);
    }
}
