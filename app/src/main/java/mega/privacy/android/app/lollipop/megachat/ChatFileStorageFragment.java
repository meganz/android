package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

public class ChatFileStorageFragment extends Fragment {

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

    DatabaseHandler dbH;
    MegaPreferences prefs;

    public ActionMode actionMode;

    CustomizedGridLayoutManager gridLayoutManager;

    ArrayList<Bitmap> thumBitmap = new ArrayList<>();
    ArrayList<String> imagesPath = new ArrayList<>();
    ArrayList<Integer> posSelected = new ArrayList<>();
    String downloadLocationDefaultPath = Util.downloadDIR;

    private static int firstVisibleInListview;

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

//        getThumbnailPath();
//        getImagesPath();
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
        else{
            String filePath = imagesPath.get(position);
            ((ChatActivityLollipop) getActivity()).uploadPicture(filePath);
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

    private void clearSelections() {
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

//    public void getThumbnailPath(){
//        String[] projection = new String[]{
//                MediaStore.Images.Media.DATA,
//        };
//
//        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//        Cursor cur1 = getActivity().managedQuery(images, projection, "", null, "");
//
//        if (cur1.moveToFirst()) {
//
//            int dataColumn = cur1.getColumnIndex(MediaStore.Images.Media.DATA);
//            do {
//                Cursor cur2 = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.MediaColumns._ID }, MediaStore.MediaColumns.DATA + "=?", new String[] {cur1.getString(dataColumn)}, null);
//
//                if (cur2 != null && cur2.moveToFirst()) {
//                    int id = cur2.getInt(cur2.getColumnIndex(MediaStore.MediaColumns._ID));
//                    thumBitmap.add(MediaStore.Images.Thumbnails.getThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null ));
//                }
//                cur2.close();
//            } while (cur1.moveToNext());
//        }
//        cur1.close();
//
//    }
//
//    public void getImagesPath(){
//
//        String[] projection1 = new String[]{
//                MediaStore.Images.Media.DATA,
//        };
//
//        Uri images1 = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//        Cursor cur3 = getActivity().managedQuery(images1, projection1, "", null, "");
//
//        if (cur3.moveToFirst()) {
//            int dataColumn = cur3.getColumnIndex(MediaStore.Images.Media.DATA);
//            do {
//                imagesPath.add(cur3.getString(dataColumn));
//            } while (cur3.moveToNext());
//        }
//        cur3.close();
//    }
//
    public void sendButton(boolean flag){
        ((ChatActivityLollipop) getActivity()).activateSendButton(flag);
        if(flag){
            List<Integer> items = adapter.getSelectedItems();
            log("*************** items: "+items.size());
        }


    }

    public void removePosition(Integer pos){
        posSelected.remove(pos);
        for(Integer element:posSelected){
            log("**** poSelected: "+element);
        }
    }

    public void addPosition(Integer pos){
        posSelected.add(pos);
        for(Integer element:posSelected){
            log("**** poSelected: "+element);
        }
    }

    public void sendImages(){
        log("####sendImages");
        String filePath;
        if(isMultipleselect()){
            log("####sendImages-isMultipleselect");

            for(Integer element:posSelected){
                log("####sendImages-element: "+element);

                filePath = imagesPath.get(element);
               ((ChatActivityLollipop) getActivity()).uploadPicture(filePath);
            }
            adapter.clearSelections();

        }
    }

}
