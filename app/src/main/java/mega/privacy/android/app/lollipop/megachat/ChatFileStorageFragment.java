package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatFileStorageAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaNode;

public class ChatFileStorageFragment extends BottomSheetDialogFragment {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    RecyclerView recyclerView;
    private RecyclerView.LayoutManager mLayoutManager;

    MegaChatFileStorageAdapter adapter;
    ChatFileStorageFragment fileStorageFragment = this;

    public static int GRID_LARGE = 3;
    public static int PADDING_GRID_LARGE = 6;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    Context context;
    ActionBar aB;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    private int height = -1;
    private boolean heightseted = false;
    private int heightReal = -1;
    private int heightDisplay;

    DatabaseHandler dbH;
    MegaPreferences prefs;

    public ActionMode actionMode;

    CustomizedGridLayoutManager gridLayoutManager;

    ArrayList<Bitmap> thumBitmap = new ArrayList<>();
    ArrayList<String> imagesPath = new ArrayList<>();
    ArrayList<Integer> posSelected = new ArrayList<>();
    String downloadLocationDefaultPath = Util.downloadDIR;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(recyclerView.getLayoutManager()!=null){
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    public static ChatFileStorageFragment newInstance() {
        ChatFileStorageFragment fragment = new ChatFileStorageFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        prefs = dbH.getPreferences();
        if (prefs != null){
            log("prefs != null");
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    log("askMe==false");
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }

        super.onCreate(savedInstanceState);
        log("after onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(!isAdded()){
            return null;
        }

        log("fragment ADDED");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if (megaApi.getRootNode() == null){
            return null;
        }

        prefs = dbH.getPreferences();
        display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        heightDisplay = outMetrics.heightPixels;

        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        View v = inflater.inflate(R.layout.fragment_filestorage, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.file_storage_grid_view_browser);
        recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
        recyclerView.setClipToPadding(false);

        recyclerView.setHasFixedSize(true);
        gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        int totalWidth = outMetrics.widthPixels;

        int gridWidth = 0;
        int realGridWidth = 0;
        int numberOfCells = 0;
        int padding = 0;

        realGridWidth = totalWidth / GRID_LARGE;
        padding = PADDING_GRID_LARGE;
        gridWidth = realGridWidth - (padding * 2);
        numberOfCells = GRID_LARGE;

            getPaths();

        if (adapter == null){
            adapter = new MegaChatFileStorageAdapter(context, this, recyclerView, aB, thumBitmap, numberOfCells, gridWidth);
            adapter.setHasStableIds(true);

        }else{
            adapter.setNumberOfCells(numberOfCells, gridWidth);
            adapter.setNodes(thumBitmap);
        }
        adapter.setMultipleSelect(false);

        mLayoutManager = new GridLayoutManager(context, numberOfCells);
        ((GridLayoutManager) mLayoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getSpanSizeOfPosition(position);
            }
        });

        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        setNodes(thumBitmap);

        if (adapter.getItemCount() == 0){
            recyclerView.setVisibility(View.VISIBLE);
        }else{
            recyclerView.setVisibility(View.VISIBLE);
        }



//        //SCROLL RecyclerView
//        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
//
//            // Keeps track of the overall vertical offset in the list
//            int verticalOffset;
//
//            // Determines the scroll UP/DOWN direction
//            boolean scrollingUp;
//
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    if (scrollingUp) {
////                        if (verticalOffset > tToolbar.getHeight()) {
////                            toolbarAnimateHide();
////                        } else {
////                            toolbarAnimateShow(verticalOffset);
////                        }
//                        log("************ 1 up ");
//                    } else {
////                        if (tToolbar.getTranslationY() < tToolbar.getHeight() * -0.6 && verticalOffset > tToolbar.getHeight()) {
////                            toolbarAnimateHide();
////                        } else {
////                            toolbarAnimateShow(verticalOffset);
////                        }
//                        log("************ 1 down ");
//
//                    }
//                }
//            }
//
//            @Override
//            public final void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                verticalOffset += dy;
//                scrollingUp = dy > 0;
////                int toolbarYOffset = (int) (dy - tToolbar.getTranslationY());
////                tToolbar.animate().cancel();
//                if (scrollingUp) {
//                    log("************ 2 up ");
//
////                    if (toolbarYOffset < tToolbar.getHeight()) {
////                        if (verticalOffset > tToolbar.getHeight()) {
////                            toolbarSetElevation(TOOLBAR_ELEVATION);
////                        }
////                        tToolbar.setTranslationY(-toolbarYOffset);
////                    } else {
////                        toolbarSetElevation(0);
////                        tToolbar.setTranslationY(-tToolbar.getHeight());
////                    }
//                } else {
//                    log("************ 2 down ");
//
////                    if (toolbarYOffset < 0) {
////                        if (verticalOffset <= 0) {
////                            toolbarSetElevation(0);
////                        }
////                        tToolbar.setTranslationY(0);
////                    } else {
////                        if (verticalOffset > tToolbar.getHeight()) {
////                            toolbarSetElevation(TOOLBAR_ELEVATION);
////                        }
////                        tToolbar.setTranslationY(-toolbarYOffset);
////                    }
//                }
//            }
//        });

//        firstVisibleInListview = ((GridLayoutManager) mLayoutManager).findFirstVisibleItemPosition();
//
//        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            int ydy = 0;
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//
//            }
//
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//
//                int currentFirstVisible = ((GridLayoutManager) mLayoutManager).findFirstVisibleItemPosition();
//
//                if(currentFirstVisible > firstVisibleInListview)
//                    log("****************UP");
//                else
//                    log("****************DOWN");
//
//                firstVisibleInListview = currentFirstVisible;
//            }
//        });
            return v;
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

    public void itemClick(int position) {
        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
        if (adapter.isMultipleSelect()){
            adapter.toggleSelection(position);
        }
    }

    private static void log(String log) {
        Util.log("ChatFileStorageFragment", log);
    }

    public RecyclerView getRecyclerView(){
        return recyclerView;
    }

    public void setNodes(ArrayList<Bitmap> thumImages){

        this.thumBitmap = thumImages;
            if (adapter != null){
                adapter.setNodes(thumImages);

                if (adapter.getItemCount() == 0){
                    recyclerView.setVisibility(View.VISIBLE);
                }else{
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
            else{
                log("grid adapter is NULL----------------");
            }
    }

    public int getItemCount(){

        if(adapter!=null){
            return adapter.getItemCount();
        }
        return 0;
    }


    public boolean showSelectMenuItem(){
        if (adapter != null){
            return adapter.isMultipleSelect();
        }

        return false;
    }

    public void clearSelections() {
        if(adapter.isMultipleSelect()){
            adapter.clearSelections();
        }
    }

    public void hideMultipleSelect() {
        log("hideMultipleSelect");
        adapter.setMultipleSelect(false);

    }

    public boolean isMultipleselect(){
        if(adapter!=null){
            return adapter.isMultipleSelect();
        }
        return false;
    }

    public void getPaths(){
        String[] projection = new String[]{
                MediaStore.Images.Media.DATA,
        };

        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur1 = getActivity().managedQuery(images, projection, "", null, "");

        if (cur1.moveToFirst()) {

            int dataColumn = cur1.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                imagesPath.add(cur1.getString(dataColumn));

                Cursor cur2 = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.MediaColumns._ID }, MediaStore.MediaColumns.DATA + "=?", new String[] {cur1.getString(dataColumn)}, null);

                if (cur2 != null && cur2.moveToFirst()) {
                    int id = cur2.getInt(cur2.getColumnIndex(MediaStore.MediaColumns._ID));
                    thumBitmap.add(MediaStore.Images.Thumbnails.getThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null ));
                }
                cur2.close();
            } while (cur1.moveToNext());
        }
        cur1.close();
    }

    public void sendButton(boolean flag){
        ((ChatActivityLollipop) getActivity()).activateSendButton(flag);
//        if(flag){
//            List<Integer> items = adapter.getSelectedItems();
//        }
    }

    public void removePosition(Integer pos){
        posSelected.remove(pos);
    }

    public void addPosition(Integer pos){
        posSelected.add(pos);
    }

    public void sendImages(){
        String filePath;
        if(isMultipleselect()){
            for(Integer element:posSelected){
                filePath = imagesPath.get(element);
               ((ChatActivityLollipop) getActivity()).uploadPicture(filePath);
            }
            adapter.clearSelections();
        }
    }

}
