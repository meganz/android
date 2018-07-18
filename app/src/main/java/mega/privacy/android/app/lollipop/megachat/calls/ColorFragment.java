package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;


public class ColorFragment extends Fragment {
    int width = 0;
    int height = 0;
    MegaChatApiAndroid megaChatApi;
    Context context;
    long chatId;
    long userHandle;
    int color;

    public RelativeLayout remoteFullScreenSurfaceView;

    public static ColorFragment newInstance(long chatId, long userHandle, int color) {
        log("newInstance");
        ColorFragment f = new ColorFragment();

        Bundle args = new Bundle();
        args.putLong("chatId", chatId);
        args.putLong("userHandle",userHandle);
        args.putInt("color",color);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        Bundle args = getArguments();
        this.chatId = args.getLong("chatId", -1);
        this.userHandle = args.getLong("userHandle", -1);
        this.color = args.getInt("color",0);
        super.onCreate(savedInstanceState);
        log("after onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if (!isAdded()) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_color, container, false);

        remoteFullScreenSurfaceView = (RelativeLayout)v.findViewById(R.id.surface_remote_video);

        if(color == 1){
            remoteFullScreenSurfaceView.setBackgroundColor(Color.BLUE);
        }else if (color == 2){
            remoteFullScreenSurfaceView.setBackgroundColor(Color.YELLOW);
        }else if (color == 3){
            remoteFullScreenSurfaceView.setBackgroundColor(Color.GREEN);
        }else{
            remoteFullScreenSurfaceView.setBackgroundColor(Color.MAGENTA);
        }

        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
    @Override
    public void onResume() {
        log("onResume");
        this.width=0;
        this.height=0;
        super.onResume();
    }

    private static void log(String log) {
        Util.log("ColorFragment", log);
    }


}
