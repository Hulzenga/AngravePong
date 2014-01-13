package com.devries.angpong;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class AudioPlayer {

    private SoundPool mPool;    
    private int mBounceSoundId;
    private Context     mContext;

    public AudioPlayer(Context context) {
        mContext = context;
        mPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        
    }

    public void playBounce() {        
        mPool.play(mBounceSoundId, 1.0f, 1.0f, 0, 0, 1.0f);            
    }

    public void loadMediaPlayer() {
        mBounceSoundId = mPool.load(mContext, R.raw.bounce, 0);
    }

    public void release() {
        mPool.release();
    }

}
