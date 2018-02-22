package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
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

public class ChatFileStorageFragment extends BottomSheetDialogFragment{

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    RecyclerView recyclerView;
    private RecyclerView.LayoutManager mLayoutManager;

    MegaChatFileStorageAdapter adapter;
    ChatFileStorageFragment fileStorageFragment = this;

    public static int GRID_LARGE = 2;

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

//    CustomizedGridLayoutManager gridLayoutManager;

    RelativeLayout rlfragment;

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
        int heightFrag = Util.scaleWidthPx(240, outMetrics);

        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        View v = inflater.inflate(R.layout.fragment_filestorage, container, false);

        rlfragment = (RelativeLayout) v.findViewById(R.id.relative_layout_frag);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, heightFrag);
        rlfragment.setLayoutParams(params);

        recyclerView = (RecyclerView) v.findViewById(R.id.file_storage_grid_view_browser);
//        recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
        recyclerView.setClipToPadding(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

//        gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();


//        int totalWidth = outMetrics.widthPixels;
//
//        int gridWidth = 0;
//        int realGridWidth = 0;
//        int numberOfCells = 0;
//        int padding = 0;
//
//        realGridWidth = totalWidth / GRID_LARGE;
//        padding = PADDING_GRID_LARGE;
//        gridWidth = realGridWidth - (padding * 2);
//        numberOfCells = GRID_LARGE;

        int numberOfCells = GRID_LARGE;
        int dimImages = heightFrag / numberOfCells;

        getPaths();

        if (adapter == null){
            adapter = new MegaChatFileStorageAdapter(context, this, recyclerView, aB, thumBitmap, dimImages);
            adapter.setHasStableIds(true);

        }else{
            adapter.setDimensionPhotos(dimImages);
            adapter.setNodes(thumBitmap);
        }
        adapter.setMultipleSelect(false);

        mLayoutManager = new GridLayoutManager(context, numberOfCells, GridLayoutManager.HORIZONTAL,false);
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
                MediaStore.Images.Media._ID
        };
        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = null;

        try {

            cursor = getActivity().managedQuery(images, projection, "", null, "");
            if (cursor.moveToFirst()) {

                int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                do {

                    imagesPath.add(cursor.getString(dataColumn));
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    thumBitmap.add(MediaStore.Images.Thumbnails.getThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null ));

                } while (cursor.moveToNext());
                cursor.close();
            }

        }catch (Exception e){

        }finally {
            if(cursor !=null){
                cursor.close();
            }
        }



    }


    public void activatedMultiselect(boolean flag){
        ((ChatActivityLollipop) getActivity()).multiselectActivated(flag);
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
