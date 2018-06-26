package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
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

    TextView emptyTextView;
//    public ProgressDialog mProgressDialog;
    public ArrayList<String> mPhotoUris;
    public ArrayList<String> imagesPath = new ArrayList<>();

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

    DatabaseHandler dbH;
    MegaPreferences prefs;

    public ActionMode actionMode;
    RelativeLayout rlfragment;

    ArrayList<Integer> posSelected = new ArrayList<>();
    String downloadLocationDefaultPath = Util.downloadDIR;


//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
////        if(recyclerView.getLayoutManager()!=null){
////            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
////        }
//    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

        int heightFrag = Util.scaleWidthPx(240, outMetrics);

        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        //setup progress dialog
//        mProgressDialog = new ProgressDialog(context);
//        mProgressDialog.setMessage("Fetching Photos...");
//        mProgressDialog.setCancelable(false);

        View v = inflater.inflate(R.layout.fragment_filestorage, container, false);
        rlfragment = (RelativeLayout) v.findViewById(R.id.relative_layout_frag);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, heightFrag);
        rlfragment.setLayoutParams(params);

        emptyTextView = (TextView) v.findViewById(R.id.empty_textview);

        recyclerView = (RecyclerView) v.findViewById(R.id.file_storage_grid_view_browser);
        recyclerView.setClipToPadding(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        if (recyclerView != null) {

            mPhotoUris = new ArrayList<>();

            int numberOfCells = GRID_LARGE;
            int dimImages = heightFrag / numberOfCells;

            mLayoutManager = new GridLayoutManager(context, numberOfCells, GridLayoutManager.HORIZONTAL,false);
            ((GridLayoutManager) mLayoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return adapter.getSpanSizeOfPosition(position);
                }
            });

            if (adapter == null){
                adapter = new MegaChatFileStorageAdapter(context, this, recyclerView, aB, mPhotoUris, dimImages);
                adapter.setHasStableIds(true);

            }else{
                adapter.setDimensionPhotos(dimImages);
                //adapter.setNodes(mPhotoUris);
                setNodes(mPhotoUris);
            }

            adapter.setMultipleSelect(false);

            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setAdapter(adapter);

            //fetch photos from gallery
            new FetchPhotosTask(fileStorageFragment).execute();

            if (adapter.getItemCount() == 0){
                recyclerView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
            }else{
                recyclerView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.GONE);
            }

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

    public void setNodes(ArrayList<String> mPhotoUris){

        this.mPhotoUris = mPhotoUris;
            if (adapter != null){
                adapter.setNodes(mPhotoUris);

                if (adapter.getItemCount() == 0){
                    recyclerView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                }else{
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.GONE);
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

//    public void getPaths(){
//        String[] projection = new String[]{
//                MediaStore.Images.Media.DATA,
//                MediaStore.Images.Media._ID
//        };
//        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//        Cursor cursor = null;
//
//        try {
//
//            cursor = getActivity().getContentResolver().query(images, projection, "", null, "");
//            if (cursor.moveToFirst()) {
//
//                int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
//                String path= null;
//                long id= 0;
//                Bitmap temp= null;
//                do {
//                    path = cursor.getString(dataColumn);
//                    imagesPath.add(path);
//                    id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
//                    temp = MediaStore.Images.Thumbnails.getThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null );
//                    temp = modifyOrientation(temp, path);
//                    thumBitmap.add(temp);
//
//                } while (cursor.moveToNext());
//                cursor.close();
//            }
//
//        }catch (Exception e){
//
//        }finally {
//            if(cursor !=null){
//                cursor.close();
//            }
//        }
//
//
//    }

//    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
//        ExifInterface ei = new ExifInterface(image_absolute_path);
//        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//
//        switch (orientation) {
//            case ExifInterface.ORIENTATION_ROTATE_90:
//                return rotate(bitmap, 90);
//
//            case ExifInterface.ORIENTATION_ROTATE_180:
//                return rotate(bitmap, 180);
//
//            case ExifInterface.ORIENTATION_ROTATE_270:
//                return rotate(bitmap, 270);
//
//            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
//                return flip(bitmap, true, false);
//
//            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
//                return flip(bitmap, false, true);
//
//            default:
//                return bitmap;
//        }
//    }
//
//    public static Bitmap rotate(Bitmap bitmap, float degrees) {
//        Matrix matrix = new Matrix();
//        matrix.postRotate(degrees);
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//    }
//
//    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
//        Matrix matrix = new Matrix();
//        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//    }


    public void activatedMultiselect(boolean flag){
        ((ChatActivityLollipop) getActivity()).multiselectActivated(flag);
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
                //filePath = mPhotoUris.get(element);
                filePath = imagesPath.get(element);
                ((ChatActivityLollipop) getActivity()).uploadPicture(filePath);
            }
            clearSelections();
            hideMultipleSelect();
        }
    }

    public static class FetchPhotosTask extends AsyncTask<Void, Void, List<String>> {
        private WeakReference<ChatFileStorageFragment> mContextWeakReference;

        public FetchPhotosTask(ChatFileStorageFragment context) {
            mContextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            ChatFileStorageFragment context = mContextWeakReference.get();
            if (context != null) {
                //context.mProgressDialog.show();
            }
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            ChatFileStorageFragment context = mContextWeakReference.get();

            if (context != null) {
                //get photos from gallery
                String[] projection = new String[]{
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media._ID
                };

                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String orderBy = MediaStore.Images.Media._ID + " DESC";

                Cursor cursor = context.getActivity().getContentResolver().query(uri, projection, "", null, orderBy);

                if (cursor != null) {
                    int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                    List<String> photoUris = new ArrayList<>(cursor.getCount());
                    while (cursor.moveToNext()) {
                        photoUris.add("file://" + cursor.getString(dataColumn));
                        context.createImagesPath(cursor.getString(dataColumn));
                    }
                    cursor.close();

                    return photoUris;
                }
            }

            return null;

        }

        @Override
        protected void onPostExecute(List<String> photoUris) {

            ChatFileStorageFragment context = mContextWeakReference.get();
            if (context != null) {
                //context.mProgressDialog.dismiss();
                if (photoUris != null && photoUris.size() > 0) {
                    context.mPhotoUris.clear();
                    context.mPhotoUris.addAll(photoUris);
                    context.adapter.notifyDataSetChanged();
                }
                if (context.adapter.getItemCount() == 0){
                    context.recyclerView.setVisibility(View.GONE);
                    context.emptyTextView.setVisibility(View.VISIBLE);
                }else{
                    context.recyclerView.setVisibility(View.VISIBLE);
                    context.emptyTextView.setVisibility(View.GONE);
                }
            }
        }
    }

    public void createImagesPath(String path){
        imagesPath.add(path);
    }

}
