package com.devries.angpong;

import android.app.Activity;
import android.os.Bundle;

public class AngravePong extends Activity {

    private PongView mPongView;
    private AudioPlayer mPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPongView = new PongView(this);
        mPlayer = new AudioPlayer(this);
        
        setContentView(mPongView);
        mPongView.setAudioPlayer(mPlayer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        mPlayer.loadMediaPlayer();
    }
    
    @Override
    protected void onPause() {        
        super.onPause();
        
        mPlayer.release();
    }

}
