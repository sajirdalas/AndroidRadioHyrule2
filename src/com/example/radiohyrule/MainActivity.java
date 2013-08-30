package com.example.radiohyrule;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.radiohyrule.WorkerService.LocalBinder;

public class MainActivity extends Activity {
    WorkerService mService;
    boolean mBound = false;
    boolean isPlaying = false;
    ImageButton playPauseButton;
    ImageView AlbumArt;
    TextView TagTitle,TagAlbum,TagArtist;
    BroadcastReceiver receiver;

	//We all need constants, don't we?
    private final String DEBUG_TAG = "RadioHyruleDebug";
    //I don't like having this constants in two places at once, but I can't get to the other one from mService before the Service is bound (I get a null pointer)
    public final String FILTER = "com.example.radiohyrule.UPDATE";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, WorkerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
        TagTitle = (TextView) findViewById(R.id.TitleTV);
        TagAlbum = (TextView) findViewById(R.id.AlbumTV);
        TagArtist = (TextView) findViewById(R.id.ArtistTV);
        AlbumArt = (ImageView) findViewById(R.id.AlbumArt);
        
        playPauseButton = (ImageButton) findViewById(R.id.PlayPauseButton);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(isPlaying){
					isPlaying = false;
					playPauseButton.setImageResource(R.drawable.play);
					mService.StopMusic();
				}else{
					isPlaying=true;
					playPauseButton.setImageResource(R.drawable.pause);
					mService.PlayMusic();
				}
				
			}

		});
        
        //Configure things for the receiver (for tags)
        receiver = new ReceiverClass();

        //Register the receiver
       registerReceiver(receiver, new IntentFilter(FILTER));

    
    }


	private void ShowResults(Intent intent) {
		Log.d(DEBUG_TAG,"Received Tile: "+intent.getStringExtra(mService.TITLE_KEY));
		Log.d(DEBUG_TAG,"Received Album: "+intent.getStringExtra(mService.ALBUM_KEY));
		Log.d(DEBUG_TAG,"Received Artist: "+intent.getStringExtra(mService.ARTIST_KEY));
		Log.d(DEBUG_TAG,"Received Image: "+intent.getStringExtra(mService.IMAGE_KEY));
		
	}
    
	private void UpdateTagSign(Intent intent){
		String Album = intent.getStringExtra(mService.ALBUM_KEY);
		String Artist = intent.getStringExtra(mService.ARTIST_KEY);
		String Title = intent.getStringExtra(mService.TITLE_KEY);
		
		TagTitle.setText(Title);
		TagArtist.setText(Html.fromHtml("<b>Artist: </b>"+Artist));
		TagAlbum.setText(Html.fromHtml("<b>Album: </b>"+Album));
	}
	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        
        //Unregister the listener
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

        
        
    }

    private void UpdateArt(String ImagePath){

    }
    
    public class ReceiverClass extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(DEBUG_TAG,"Broadcast received");
			ShowResults(intent);
			UpdateTagSign(intent);
			UpdateArt(intent.getStringExtra(mService.IMAGE_KEY));
		}
    	
    }


}
