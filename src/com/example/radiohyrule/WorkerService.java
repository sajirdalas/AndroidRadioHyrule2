package com.example.radiohyrule;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class WorkerService extends Service implements OnPreparedListener{
	
	//Our constants for logs and broadcasts
	
	private final String DEBUG_TAG = "RadioHyruleDebug";
	
	public final String ALBUM_KEY = "CurrentAlbum";
	public final String TITLE_KEY = "CurrentSong";
	public final String ARTIST_KEY = "CurrentArtist";
	public final String IMAGE_KEY = "CurrentImage";
	public final String FILTER = "com.example.radiohyrule.UPDATE";

	
	//Bunch of variables we will need
	MediaPlayer mePlayer;
	private final IBinder mBinder = new LocalBinder();
	String songTitle, songAlbum, songArtist, imagePath;
	int songStart,songDuration,timeToWait;
	TextView sign;
	LocalBroadcastManager broadcaster;
	String oldTitle = "";
	boolean ThreadRunning = false;

	
	public void NotifyConnection(){
		Toast.makeText(getApplicationContext(), "Working", Toast.LENGTH_LONG).show();;
		Log.d("Debug", "Service Working");
	}
	
	public void PlayMusic(){
		mePlayer.prepareAsync();
		//Thread managing
		ThreadRunning = true;
		TagMonitor monit = new TagMonitor();
		monit.start();
	}
	
	public void StopMusic(){
		ThreadRunning = false;
		mePlayer.stop();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mePlayer = new MediaPlayer();
		mePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mePlayer.setOnPreparedListener(this);
		try {
			mePlayer.setDataSource(getString(R.string.RadioSource));
		} catch (IllegalArgumentException e) {
			Log.e("Error in app", "Ilegal Argument");
			e.printStackTrace();
		} catch (SecurityException e) {
			Log.e("Error in app", "Security Exception");
			e.printStackTrace();
		} catch (IllegalStateException e) {
			Log.e("Error in app", "Ilegal State Exception");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("Error in app", "IO Exception");
			e.printStackTrace();
		}
		
		//Broadcast managing for the UI control back on the activity
		 broadcaster = LocalBroadcastManager.getInstance(this);
		
		//Thread managing
		ThreadRunning = false;
		TagMonitor monit = new TagMonitor();
		monit.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mePlayer.isPlaying()){
		mePlayer.stop();}
		mePlayer.release();
	}


	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	public class LocalBinder extends Binder {
	        WorkerService getService() {
	            // Return this instance of LocalService so clients can call public methods
	            return WorkerService.this;
	        }
	    }

	@Override
	public void onPrepared(MediaPlayer mp) {
		mePlayer.start();
	}


	public class TagMonitor extends Thread {

		@SuppressWarnings("static-access")
		public void run() {
			getOnlineTags();
			SendBroadcast(songTitle, songArtist, songAlbum, imagePath);
			try {
				Thread.currentThread().sleep(timeToWait*1000);
			} catch (InterruptedException e) {
				Log.e("Error","Sleep interrupted");
				e.printStackTrace();
			}
			
			while (ThreadRunning) {
				getOnlineTags();

				if (!oldTitle.equals(songTitle)) {
					oldTitle = songTitle;
					SendBroadcast(songTitle, songArtist, songAlbum, imagePath);
				}
				
				try {
					Thread.currentThread().sleep(timeToWait*1000);
				} catch (InterruptedException e) {
					Log.e("Error","Sleep interrupted");
					e.printStackTrace();
				}
				
				ThreadRunning=false;
			}
		}

		private void getOnlineTags() {
			try {
				URL SongDataUrl = new URL(getString(R.string.TagURL));
				HttpURLConnection connection = (HttpURLConnection) SongDataUrl
						.openConnection();
				connection.connect();

				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					InputStream inputS = connection.getInputStream();
					Reader reader = new InputStreamReader(inputS);
					char[] charArray = new char[connection.getContentLength()];
					reader.read(charArray);
					JSONObject JSONData = new JSONObject(new String(charArray));
					songTitle = JSONData.getString("title");
					Log.d(DEBUG_TAG, "Tile: " + songTitle);
					songAlbum = JSONData.getString("album");
					Log.d(DEBUG_TAG, "Album: " + songAlbum);
					songArtist = JSONData.getString("artist");
					Log.d(DEBUG_TAG, "Artist: " + songArtist);
					songStart = JSONData.getInt("started");
					Log.d(DEBUG_TAG, "Start: " + Integer.toString(songStart));
					songDuration = JSONData.getInt("duration");
					Log.d(DEBUG_TAG,"Duration: " + Integer.toString(songDuration));
					int ExpectedEnd = songStart+songDuration+1;
					int CurrentTime = (int) (System.currentTimeMillis() / 1000L);
					timeToWait = ExpectedEnd - CurrentTime;
					Log.d(DEBUG_TAG,"To wait: "+Integer.toString(timeToWait));
					
					//Now to get the art
					Connection con = Jsoup.connect(getString(R.string.Site));
					con.timeout(5000);
					Document doc = con.get();
					Elements imagen = doc.select("#nowplaying-img");
					imagePath = imagen.attr("src");
				}

			} catch (MalformedURLException e) {
				Log.e("Error", "Malformed URL");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("Error", "IO Exception");
				e.printStackTrace();
			} catch (JSONException e) {
				Log.e("Error", "JSON exception");
				e.printStackTrace();
			}
		}

	}

	public void SendBroadcast(String Title, String Artist, String Album, String Image){
		
		Log.d(DEBUG_TAG,"Broadcast sent");
		Intent intent = new Intent(FILTER);
		intent.putExtra(ARTIST_KEY, Artist);
		intent.putExtra(ALBUM_KEY, Album);
		intent.putExtra(TITLE_KEY, Title);
		intent.putExtra(IMAGE_KEY, Image);
		sendBroadcast(intent);
	}
	
}
