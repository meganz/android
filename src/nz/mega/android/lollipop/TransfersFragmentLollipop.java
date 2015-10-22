package nz.mega.android.lollipop;

import java.util.ArrayList;
import java.util.List;

import nz.mega.android.MegaApplication;
import nz.mega.android.R;
import nz.mega.android.UploadService;
import nz.mega.android.lollipop.FileBrowserFragmentLollipop.RecyclerViewOnGestureListener;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class TransfersFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener{

	Context context;
	ActionBar aB;
	RecyclerView listView;
	MegaTransfersLollipopAdapter adapter;
	
	MegaApiAndroid megaApi;
	
	ImageView emptyImage;
	TextView emptyText;
	ImageView pauseImage;
	TextView pauseText;
	
	LinearLayout outSpaceLayout=null;
	TextView outSpaceText;
	Button outSpaceButton;
	int usedSpacePerc;
	
	boolean pause = false;
	private RecyclerView.LayoutManager mLayoutManager;
	
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
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		aB.setTitle(getResources().getString(R.string.section_transfers));					
//		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		
		View v = inflater.inflate(R.layout.fragment_transfers, container, false);
		
		
//		if (transfersListArray == null){
//			tL = megaApi.getTransfers();
//			transfersListArray = new SparseArray<TransfersHolder>();
//			
//			for (int i = 0; i< tL.size(); i++){
//				MegaTransfer t = tL.get(i);
//				TransfersHolder th = new TransfersHolder();
//				
//				th.setName(new String(t.getFileName()));
//				
//				transfersListArray.put(t.getTag(), th);
//			}
//		}
		tL = megaApi.getTransfers();
		
		listView = (RecyclerView) v.findViewById(R.id.transfers_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator());		
		
//		adapter = new MegaTransfersAdapter(context, transfersListArray, aB);
		adapter = new MegaTransfersLollipopAdapter(context, this, tL, aB);
		adapter.setPositionClicked(-1);
		
		outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_tranfers);
		outSpaceText =  (TextView) v.findViewById(R.id.out_space_text_tranfers);
		outSpaceButton = (Button) v.findViewById(R.id.out_space_btn_tranfers);
		outSpaceButton.setVisibility(View.VISIBLE);
		outSpaceButton.setOnClickListener(this);
		
		usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
		
		if(usedSpacePerc>95){
			//Change below of ListView
			log("usedSpacePerc>95");
//			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//			p.addRule(RelativeLayout.ABOVE, R.id.out_space);
//			listView.setLayoutParams(p);
			outSpaceLayout.setVisibility(View.VISIBLE);
			outSpaceLayout.bringToFront();
			
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {					
				
				@Override
				public void run() {
					log("BUTTON DISAPPEAR");
					log("altura: "+outSpaceLayout.getHeight());
					
					TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, outSpaceLayout.getHeight());
					animTop.setDuration(2000);
					animTop.setFillAfter(true);
					outSpaceLayout.setAnimation(animTop);
				
					outSpaceLayout.setVisibility(View.GONE);
					outSpaceLayout.invalidate();
//					RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//					p.addRule(RelativeLayout.ABOVE, R.id.buttons_layout);
//					listView.setLayoutParams(p);
				}
			}, 15 * 1000);
			
		}	
		else{
			outSpaceLayout.setVisibility(View.GONE);
		}
		
		
		adapter.setMultipleSelect(false);
		
		listView.setAdapter(adapter);
		
		emptyImage = (ImageView) v.findViewById(R.id.transfers_empty_image);
		pauseImage = (ImageView) v.findViewById(R.id.transfers_pause_image);
		emptyText = (TextView) v.findViewById(R.id.transfers_empty_text);
		pauseText = (TextView) v.findViewById(R.id.transfers_pause_text);
		
		emptyImage.setImageResource(R.drawable.ic_no_active_transfers);
		pauseImage.setImageResource(R.drawable.ic_pause_tranfers);
		emptyText.setText(getString(R.string.transfers_empty));
		pauseText.setText(getString(R.string.transfers_pause));
		
		pauseImage.setVisibility(View.GONE);
		pauseText.setVisibility(View.GONE);
		
		if (pause){
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.GONE);
			
			pauseImage.setVisibility(View.VISIBLE);
			pauseText.setVisibility(View.VISIBLE);
		}
		else{
			if (tL.size() == 0){
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
				((ManagerActivityLollipop)context).hideTransfersIcons();
			}
			else{
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
				((ManagerActivityLollipop)context).setPauseIconVisible(true);
			}
			
			pauseImage.setVisibility(View.GONE);
			pauseText.setVisibility(View.GONE);
		}    
		 
//		refreshTransfers();
		
		return v;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.out_space_btn_tranfers:
				((ManagerActivityLollipop)getActivity()).upgradeAccountButton();
				break;
		}
	}
	
    public void itemClick(AdapterView<?> parent, View view, int position, long id) {
		
		if (adapter != null){
			adapter.threeDotsClick(position);
		}	
		
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
	
//	public void setTransfers(SparseArray<TransfersHolder> tl){
//		transfersListArray = tl;
//		if (adapter != null){
//			adapter.setTransfers(transfersListArray);
//		}
//	}
	
	public void setTransfers(ArrayList<MegaTransfer> _transfers){
		this.tL = _transfers;
		
		if (adapter != null){
			if (tL == null){
				adapter.setPositionClicked(-1);
			}
			else{
				if (tL.size() == 0){
					adapter.setPositionClicked(-1);
					((ManagerActivityLollipop)context).hideTransfersIcons();
				}
				else{
					((ManagerActivityLollipop)context).setPauseIconVisible(true);
				}
			}
			adapter.setTransfers(tL);
		}
		
		if (emptyImage != null){
			if (pause){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
				
				pauseImage.setVisibility(View.VISIBLE);
				pauseText.setVisibility(View.VISIBLE);
			}
			else{
				if (tL.size() == 0){
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
					((ManagerActivityLollipop)context).hideTransfersIcons();
				}
				else{
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
					((ManagerActivityLollipop)context).setPauseIconVisible(true);
				}	
				
				pauseImage.setVisibility(View.GONE);
				pauseText.setVisibility(View.GONE);
			}
		}
	}
	
	public void setCurrentTransfer(MegaTransfer mT){
		log("setCurrentTransfer");
		if (adapter != null){
			adapter.setCurrentTransfer(mT);
		}
	}
	
	public void setPause(boolean pause){
		this.pause = pause;
		
		if (adapter != null){
			tL = megaApi.getTransfers();
			adapter.setTransfers(tL);
		}
		
		if (emptyImage != null){ //This means that the view has been already created
			if (pause){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
				
				pauseImage.setVisibility(View.VISIBLE);
				pauseText.setVisibility(View.VISIBLE);
			}
			else{
				if (tL.size() == 0){
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
					((ManagerActivityLollipop)context).hideTransfersIcons();
				}
				else{
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
					((ManagerActivityLollipop)context).setPauseIconVisible(true);
				}
				
				pauseImage.setVisibility(View.GONE);
				pauseText.setVisibility(View.GONE);
			}
		}
	} 
	
	
	public void setNoActiveTransfers(){
		this.pause = false;
		if (emptyImage != null){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
			pauseImage.setVisibility(View.GONE);
			pauseText.setVisibility(View.GONE);
			((ManagerActivityLollipop)context).setPauseIconVisible(false);
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
