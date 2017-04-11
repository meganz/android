package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaTransfersLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaTransfer;


public class TransfersFragmentLollipop extends Fragment implements RecyclerView.OnItemTouchListener{

	Context context;
	ActionBar aB;
	RecyclerView listView;
	MegaTransfersLollipopAdapter adapter;
	
	MegaApiAndroid megaApi;
	TextView contentText;
	ImageView emptyImage;
	TextView emptyText;

	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	boolean pause = false;
	LinearLayoutManager mLayoutManager;
	
	TransfersFragmentLollipop transfersFragment = this;
	
//	SparseArray<TransfersHolder> transfersListArray = null;

	ArrayList<MegaTransfer> tL = null;

	private Handler handler;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		handler = new Handler();

		tL = new ArrayList<MegaTransfer>();

		super.onCreate(savedInstanceState);
		log("onCreate");		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {  
		
		super.onCreateView(inflater, container, savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		
		aB.setTitle(getResources().getString(R.string.section_transfers));
		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
//		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
	    
		View v = inflater.inflate(R.layout.fragment_transfers, container, false);


		listView = (RecyclerView) v.findViewById(R.id.transfers_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.setHasFixedSize(true);
		listView.setItemAnimator(new DefaultItemAnimator());

		emptyImage = (ImageView) v.findViewById(R.id.transfers_empty_image);
		emptyText = (TextView) v.findViewById(R.id.transfers_empty_text);

		emptyImage.setImageResource(R.drawable.ic_no_active_transfers);
		emptyText.setText(getString(R.string.transfers_empty));

		setTransfers();
		
//		adapter = new MegaTransfersAdapter(context, transfersListArray, aB);
		adapter = new MegaTransfersLollipopAdapter(context, this, tL, aB);

		adapter.setMultipleSelect(false);
		listView.setAdapter(adapter);

		return v;
	}

	public void setTransfers(){
		log("setTransfers");
		synchronized(((ManagerActivityLollipop)context).transfersInProgressSync) {
			tL.addAll(((ManagerActivityLollipop)context).transfersInProgressSync);
		}
		if(!(((ManagerActivityLollipop)context).transfersCompleted.isEmpty())){
			log("Add completed transfers");
			tL.addAll(((ManagerActivityLollipop)context).transfersCompleted);
		}

		if (tL.size() == 0){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
		else{
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}
	}

	public void refreshAllTransfers(){
		log("refreshAllTransfers");
		tL.clear();

		synchronized(((ManagerActivityLollipop)context).transfersInProgressSync) {
			tL.addAll(((ManagerActivityLollipop)context).transfersInProgressSync);
		}
		if(!(((ManagerActivityLollipop)context).transfersCompleted.isEmpty())){
			log("Add completed transfers");
			tL.addAll(((ManagerActivityLollipop)context).transfersCompleted);
		}

		if (tL.size() == 0){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
		else{
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}

		adapter.notifyDataSetChanged();
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

	public int onBackPressed(){
		
		if (adapter == null){
			return 0;
		}
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else{
			return 0;
		}
	}

	public void transferUpdate(MegaTransfer transfer){
        log("transferUpdate");
        if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
            tL.set(0, transfer);
            adapter.notifyItemChanged(0);
        }
        else {
            if(((ManagerActivityLollipop)context).downloadInProgress!=-1){
                tL.set(1, transfer);
                adapter.updateProgress(1);
            }
            else{
                tL.set(0, transfer);
                adapter.updateProgress(0);
            }
        }
    }

	public void setPause(boolean pause){
		this.pause = pause;
		
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
		
	}

	
	public void setNoActiveTransfers(){
		this.pause = false;
		if (emptyImage != null){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
	}
	
	public void cancelTransferConfirmation (MegaTransfer t){

		((ManagerActivityLollipop) context).cancelTransfer(t);
	}

	private static void log(String log) {
		Util.log("TransfersFragmentLollipop", log);
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}
}
