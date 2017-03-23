package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
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
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
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
	RelativeLayout contentTextLayout;
	TextView contentText;
	ImageView emptyImage;
	TextView emptyText;
	ImageView pauseImage;
	TextView pauseText;
	ProgressBar progressBar;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
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
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new MegaLinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator());		
		
//		adapter = new MegaTransfersAdapter(context, transfersListArray, aB);
		adapter = new MegaTransfersLollipopAdapter(context, this, tL, aB);
		adapter.setPositionClicked(-1);
		
		contentTextLayout = (RelativeLayout) v.findViewById(R.id.transfers_list_content_text_layout);
		progressBar = (ProgressBar) v.findViewById(R.id.transfers_list_download_progress_bar);
		progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
		contentText = (TextView) v.findViewById(R.id.transfers_list_content_text);			
		//Margins
		RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
		contentTextParams.setMargins(Util.scaleWidthPx(78, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
		contentText.setLayoutParams(contentTextParams);

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
			contentTextLayout.setVisibility(View.VISIBLE);
			pauseImage.setVisibility(View.VISIBLE);
			pauseText.setVisibility(View.VISIBLE);
		}
		else{
			if (tL.size() == 0){
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
			}
			else{
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
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
				}				
			}
			adapter.setTransfers(tL);
		}
		
		if (emptyImage != null){
			if (pause){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.VISIBLE);
				pauseImage.setVisibility(View.VISIBLE);
				pauseText.setVisibility(View.VISIBLE);
			}
			else{
				if (tL.size() == 0){
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					listView.setVisibility(View.GONE);
				}
				else{
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
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
				contentTextLayout.setVisibility(View.VISIBLE);
				pauseImage.setVisibility(View.VISIBLE);
				pauseText.setVisibility(View.VISIBLE);
			}
			else{
				if (tL.size() == 0){
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					listView.setVisibility(View.GONE);
				}
				else{
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
				}
				
				pauseImage.setVisibility(View.GONE);
				pauseText.setVisibility(View.GONE);
			}
		}
	} 
	
	public void updateProgressBar(int progress){
		if(progressBar!=null){
			progressBar.setProgress(progress);
		}			
	}
	
	public void setNoActiveTransfers(){
		this.pause = false;
		if (emptyImage != null){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			contentTextLayout.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
			pauseImage.setVisibility(View.GONE);
			pauseText.setVisibility(View.GONE);
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
