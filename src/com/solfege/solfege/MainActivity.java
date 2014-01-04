package com.solfege.solfege;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;


public class MainActivity extends Activity {
	
	private static final String LOG_TAG = "Solfege";
	static final int SET_PROBABILITY_SETTINGS = 1;
	public final static String DEGREE_PROBABILITY = "com.example.solfege.DEGREE_PROBABILITY";
	public final static String RHYTHM_PROBABILITY = "com.example.solfege.RHYTHM_PROBABILITY";
	private PdUiDispatcher dispatcher;
	private RightHand rightHand;
	private LeftHand leftHand;
	private WebView notationWebView;
	
	//for recording and playing sounds
	private MediaRecorder mRecorder = null;
	private MediaPlayer   mPlayer = null;
	private static String mFileName = null;
	boolean mStartRecording = true;
	boolean mStartPlaying = true;
	   
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//initialize right and left hands
		rightHand = new RightHand();
		leftHand = new LeftHand();
		
		// load the notation view
		notationWebView = (WebView) findViewById(R.id.partitionHtml);
		notationWebView.loadUrl("file:///android_asset/solfegeHtmlView.htm");
		
		//enable javascript
		notationWebView.getSettings().setJavaScriptEnabled(true);
		notationWebView.addJavascriptInterface(leftHand, "lefthand");
		notationWebView.addJavascriptInterface(rightHand, "righthand");
		
		notationWebView.setWebChromeClient(new WebChromeClient() {
			  public boolean onConsoleMessage(ConsoleMessage cm) {
			    Log.e(LOG_TAG, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId() );
			    return true;
			  }
			});		

		//Initialize PD path
		try {
			initPd();
			loadPatch();
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
			finish();
		}
		
		//prepare for audio file recording
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";
	}

	private void initPd() throws IOException {
		// Configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate();
		PdAudio.initAudio(sampleRate, 0, 2, 8, true);

		// Create and install the dispatcher
		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver(dispatcher);
	}

	private void loadPatch() throws IOException {
		File dir = getFilesDir();
//		IoUtils.extractZipResource(getResources().openRawResource(R.raw.no_toggle), dir, true);
//		File patchFile = new File(dir, "solfege.pd");
		IoUtils.extractZipResource(getResources().openRawResource(R.raw.toggle), dir, true);
		File patchFile = new File(dir, "solfege.pd");
		int val = PdBase.openPatch(patchFile.getAbsolutePath());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onPlayButtonClick(View view) {
		
		//this is how to call javascript functions from here... it works! 
		//notationWebView.loadUrl("javascript:createRoot()");
		
		int curRootNote = rightHand.getCurrentMidiRootNote();
		if (curRootNote!= -1){
			PdBase.sendFloat("midinote1", curRootNote);
			PdBase.sendBang("rootTrigger");			
		}
		
		int curGuessNote = rightHand.getCurrentMidiGuessNote();
		if (curGuessNote != -1){
			PdBase.sendFloat("midinote2", curGuessNote);
			PdBase.sendBang("guessTrigger");
		}
	}
	
    public void onRecordSoundButtonClick(View v) {
        onRecord(mStartRecording);
        if (mStartRecording) {
        	((Button) v).setText("Stop recording");
        } else {
        	((Button) v).setText("Start recording");
        }
        mStartRecording = !mStartRecording;
    }
    
    public void onPlaySoundButtonClick(View v) {
        onPlay(mStartPlaying);
        if (mStartPlaying) {
        	((Button) v).setText("Stop playing");
        } else {
        	((Button) v).setText("Start playing");
        }
        mStartPlaying = !mStartPlaying;
    }
	
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }
	
	public void onSettingsButtonClick(View view) {
		
		//create an intent to start the settings activity
		Intent intent = new Intent(this, SettingsActivity.class);
		
		intent.putExtra(DEGREE_PROBABILITY, rightHand.getDegreeProbability());
		intent.putExtra(RHYTHM_PROBABILITY, rightHand.getRhythmProbability());
		
		//create the activity, asking for a result
	    //startActivity(intent);
		startActivityForResult(intent,  SET_PROBABILITY_SETTINGS);
		
	}
	
	//called when other activities end
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	//if the activity that ended what the settings activity
        if (requestCode == SET_PROBABILITY_SETTINGS) {
            if (resultCode == RESULT_OK) {
            	rightHand.setDegreeProbability(data.getIntArrayExtra(MainActivity.DEGREE_PROBABILITY));
            	rightHand.setRhythmProbability(data.getIntArrayExtra(MainActivity.RHYTHM_PROBABILITY));
            }
        }
    }

	@Override
	protected void onResume() {
		super.onResume();
		PdAudio.startAudio(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PdAudio.stopAudio();
		
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PdAudio.release();
		PdBase.release();
	}
}
