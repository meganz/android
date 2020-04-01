package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatFileStorageAdapter;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ChatFileStorageFragment extends BottomSheetDialogFragment{

    RecyclerView recyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    TextView emptyTextView;
    public ArrayList<String> mPhotoUris;
    public ArrayList<String> imagesPath = new ArrayList<>();
    MegaChatFileStorageAdapter adapter;
    ChatFileStorageFragment fileStorageFragment = this;
    public static int GRID_LARGE = 2;
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
    String downloadLocationDefaultPath;
    FloatingActionButton sendIcon;

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

        dbH = DatabaseHandler.getDbHandler(getActivity());

        prefs = dbH.getPreferences();

        downloadLocationDefaultPath = getDownloadLocation(context);

        super.onCreate(savedInstanceState);
        logDebug("After onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(!isAdded()){
            return null;
        }

        logDebug("Fragment ADDED");

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        prefs = dbH.getPreferences();
        display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        scaleW = getScaleW(outMetrics, density);
        scaleH = getScaleH(outMetrics, density);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int heightFrag = displayMetrics.heightPixels / 2 - getActionBarHeight(((Activity)context), getResources());

        View v = inflater.inflate(R.layout.fragment_filestorage, container, false);
        rlfragment = (RelativeLayout) v.findViewById(R.id.relative_layout_frag);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, heightFrag);
        rlfragment.setLayoutParams(params);
        emptyTextView = (TextView) v.findViewById(R.id.empty_textview);
        recyclerView = (RecyclerView) v.findViewById(R.id.file_storage_grid_view_browser);
        recyclerView.setClipToPadding(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        sendIcon = (FloatingActionButton) v.findViewById(R.id.send_file_icon_chat);
        sendIcon.setVisibility(View.GONE);
        sendIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImages();
            }
        });

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

            if (adapter == null) {
                adapter = new MegaChatFileStorageAdapter(context, this, aB, mPhotoUris, dimImages);
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

           checkAdapterItems(this);
        }
            return v;
    }

    private static void checkAdapterItems(ChatFileStorageFragment context){
        context.sendIcon.setVisibility(View.GONE);
        if (context.adapter.getItemCount() == 0){
            context.recyclerView.setVisibility(View.GONE);
            context.emptyTextView.setVisibility(View.VISIBLE);
            return;
        }
        if(context.adapter.isMultipleSelect()){
            context.sendIcon.setVisibility(View.VISIBLE);
        }
        context.recyclerView.setVisibility(View.VISIBLE);
        context.emptyTextView.setVisibility(View.GONE);
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

    public void updateIconSend(boolean isVisible) {
        logDebug("isVisible: " + isVisible);
        if (isVisible) {
            sendIcon.setVisibility(View.VISIBLE);
        } else {
            sendIcon.setVisibility(View.GONE);
        }
    }

    public void itemClick(int position) {
        logDebug("Position: " + position);
        if (adapter.isMultipleSelect()){
            adapter.toggleSelection(position);
        }
    }

    public RecyclerView getRecyclerView(){
        return recyclerView;
    }

    public void setNodes(ArrayList<String> mPhotoUris){

        this.mPhotoUris = mPhotoUris;
        if(adapter == null) return;

        adapter.setNodes(mPhotoUris);
        checkAdapterItems(this);

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
        if((adapter == null)||(!adapter.isMultipleSelect())) return;
        adapter.clearSelections();

    }

    public void hideMultipleSelect() {
        logDebug("hideMultipleSelect");
        adapter.setMultipleSelect(false);

    }

    public boolean isMultipleselect(){
        if(adapter!=null){
            return adapter.isMultipleSelect();
        }
        return false;
    }


    public void removePosition(Integer pos){
        posSelected.remove(pos);
    }

    public void addPosition(Integer pos){
        posSelected.add(pos);
    }

    public void sendImages(){
        String filePath;
        if (isMultipleselect()) {
            ((ChatActivityLollipop) getActivity()).setIsWaitingForMoreFiles(true);
            for (int i = 0; i < posSelected.size(); i++) {
                filePath = imagesPath.get(posSelected.get(i));
                if (i == posSelected.size() - 1) {
                    ((ChatActivityLollipop) getActivity()).setIsWaitingForMoreFiles(false);
                }
                ((ChatActivityLollipop) getActivity()).uploadPictureOrVoiceClip(filePath);
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
                Cursor cursor = null;
                try {
                    cursor = context.getActivity().getContentResolver().query(uri, projection, "", null, orderBy);

                    if (cursor != null) {
                        int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                        List<String> photoUris = new ArrayList<>(cursor.getCount());
                        while (cursor.moveToNext()) {
                            photoUris.add("file://" + cursor.getString(dataColumn));
                            context.createImagesPath(cursor.getString(dataColumn));
                        }

                        return photoUris;
                    }
                } catch (Exception ex) {
                    logError("Exception is thrown", ex);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
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

                checkAdapterItems(context);

            }
        }
    }



    public void createImagesPath(String path){
        imagesPath.add(path);
    }
}