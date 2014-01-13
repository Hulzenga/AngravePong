package com.devries.angpong;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class PongView extends View {

    /*
     * Utilities
     */
    private Context                   mContext;
    private Random                    mRandom;

    private AudioPlayer               mPlayer;

    /*
     * Graphics variables
     */
    private Paint                     mSimplePaint;
    private Paint                     mAnouncePaint;
    private Bitmap                    mBallBitmap;
    private Bitmap                    mAngraveBitmap;

    /*
     * Ball variables
     */
    private static final float        BALL_SIZE                = 76.0f;
    private static final float        BALL_DRAW_OFFSET         = BALL_SIZE / 2.0f;

    private float                     mBallX;
    private float                     mBallY;
    private float                     mBallStartX;
    private float                     mBallStartY;

    private float                     mBallStartSpeed;
    private float                     mBallVx;
    private float                     mBallVy;

    /*
     * Angrave forehead variables
     */
    private static final float        ANGRAVE_WIDTH            = 200.0f;
    private static final float        ANGRAVE_DRAW_OFFSET      = ANGRAVE_WIDTH / 2.0f;
    private static final float        ANGRAVE_HEIGHT           = 75.0f;
    private static final float        ANGRAVE_COLLISION_HEIGHT = 50.0f;

    private float                     mAngraveV;

    private float                     mAngraveDrawingHeight;
    private float                     mAngraveCollisionHeight;

    private float                     mAngraveLeftBound        = ANGRAVE_DRAW_OFFSET;
    private float                     mAngraveRightBound;

    private float                     mAngraveX;
    private float                     mAngraveStartX;
    private float                     mAngraveTargetX;
    private boolean                   mAngraveTargeting;

    /*
     * View boundary variables
     */
    private int                       mWidth;
    private int                       mHeight;
    private float                     mTopCollisionY           = BALL_DRAW_OFFSET;
    private float                     mBottomCollisionY;
    private float                     mLeftCollisionX          = BALL_DRAW_OFFSET;
    private float                     mRightCollisionX;

    /*
     * Game thread control and timing variables
     */
    private Thread                    mGameThread;
    private long                      mLastFrameTime;
    private static final long         FRAME_TIME_TARGET        = 16;

    /*
     * Game states
     */
    private static final int          GAME_NOT_STARTED         = 0;
    private static final int          GAME_PLAYING             = 1;
    private static final int          GAME_LOST                = 2;
    private static final int          GAME_PAUSED              = 4;

    private int                       mGameState               = GAME_NOT_STARTED;

    private int                       mScore                   = 0;
    private int                       mHighScore               = 0;
    private boolean                   mGotHighScore            = false;
    private int                       mGamesLost               = 0;

    /*
     * Game difficulty cuve settings
     */
    private static final float        BALL_BOUNCE_FACTOR       = 1.04f;
    private static final float        ANGRAVE_BALL_SPEED_FRAC  = 0.85f;
    private static final double       BOUNCE_ANGLE_RANDOMIZER  = 14.0f / 180.0f * Math.PI;
    private static final float        MAX_SPEED_WIDTH_FACTOR   = 1.0f / 1000.0f;
    private static final float        MAX_RETURN_ANGLE         = (float) Math.PI / 4.0f;
    private float                     mMaxSpeed;

    /*
     * Insults
     */
    private static final List<String> Insults1                 = Arrays.asList("pathetic",
                                                                       "worse then an 8-bit finite automaton",
                                                                       "gerbils have done better",
                                                                       "are you even trying ?",
                                                                       "boring...", "I doubt you can do better",
                                                                       "I'm sorry (not)");
    private static final List<String> Insults2                 = Arrays.asList("you failed the Turing test",
                                                                       "I bet you failed Calculus",
                                                                       "LOSER !", "Getting mad ?",
                                                                       "is this too hard for you ?",
                                                                       "does baby need a booboo ?", "not even close",
                                                                       "just give up already");
    private static final List<String> Insults3                 = Arrays.asList("your mother is a hamster",
                                                                       "I fart in your general direcion",
                                                                       "your father smells of elderberries",
                                                                       "do I need to taunt you a second time ?",
                                                                       "you English pigdog",
                                                                       "Go and boil your bottoms",
                                                                       "you son of a silly person",
                                                                       "I blow my nose at you");

    private static List<String>       someInsults              = Insults1;

    private static List<String>       moreInsults              = new ArrayList<String>();
    {
        moreInsults.addAll(Insults1);
        moreInsults.addAll(Insults2);
    }
    private static List<String>       allInsults               = new ArrayList<String>();
    {
        allInsults.addAll(Insults1);
        allInsults.addAll(Insults2);
        allInsults.addAll(Insults3);
    }

    public PongView(Context context) {
        super(context);

        // set background to black
        this.setBackgroundColor(Color.BLACK);

        // create paint object for text and bitmap drawing
        mSimplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSimplePaint.setColor(Color.WHITE);
        mSimplePaint.setTextSize(25.0f);

        // create center announcement paint
        mAnouncePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAnouncePaint.setTextAlign(Paint.Align.CENTER);
        mAnouncePaint.setColor(Color.rgb(255, 240, 240));
        mAnouncePaint.setTextSize(45.0f);

        // load the ball and forehead bitmap
        mBallBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.android_ball);
        mAngraveBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.angrave);

        // load randomizer;
        mRandom = new Random();
    }

    public void setAudioPlayer(AudioPlayer player) {
        mPlayer = player;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw score
        canvas.drawText("Score: " + mScore, 10.0f, 35.0f, mSimplePaint);

        if (mGameState == GAME_NOT_STARTED) {
            canvas.drawText("Touch the screen to start playing Pong !", mWidth / 2.0f, mHeight * (2.0f / 5.0f),
                    mAnouncePaint);
        } else if (mGameState == GAME_LOST) {
            canvas.drawText(getInsult(), mWidth / 2.0f, mHeight * (2.0f / 5.0f), mAnouncePaint);
        }
        // draw forehead and ball
        canvas.drawBitmap(mAngraveBitmap, mAngraveX - ANGRAVE_DRAW_OFFSET, mAngraveDrawingHeight, mSimplePaint);
        canvas.drawBitmap(mBallBitmap, mBallX - BALL_DRAW_OFFSET, mBallY - BALL_DRAW_OFFSET, mSimplePaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // view width and height
        mWidth = w;
        mHeight = h;

        // ball and forehead starting positions
        mBallStartX = mWidth / 2.0f;
        mBallStartY = mHeight * (1.0f / 5.0f);
        mAngraveStartX = mBallStartX;

        // update collision bounds
        mBottomCollisionY = mHeight - BALL_DRAW_OFFSET;
        mRightCollisionX = mWidth - BALL_DRAW_OFFSET;
        mAngraveCollisionHeight = mHeight - ANGRAVE_COLLISION_HEIGHT - BALL_DRAW_OFFSET;

        // update forehead drawing height
        mAngraveDrawingHeight = mHeight - ANGRAVE_HEIGHT;

        // update forehead position bounds
        mAngraveRightBound = mWidth - ANGRAVE_DRAW_OFFSET;

        // set start speed
        mBallStartSpeed = mHeight / 1000.0f;

        // set maximum speed
        mMaxSpeed = MAX_SPEED_WIDTH_FACTOR * mWidth;

        // set game into start state
        initializeGameEnvironment();

        // call invalidate to ensure view is drawn
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:

            if (mGameState != GAME_PLAYING) {
                start();
            }

            mAngraveTargeting = true;
        case MotionEvent.ACTION_MOVE:
            float x = event.getX();
            if (x < mAngraveLeftBound) {
                mAngraveTargetX = mAngraveLeftBound;
            } else if (x > mAngraveRightBound) {
                mAngraveTargetX = mAngraveRightBound;
            } else {
                mAngraveTargetX = x;
            }
            break;
        case MotionEvent.ACTION_UP:
            mAngraveTargeting = false;
            break;
        }

        return true;
    }

    public void start() {
        mGameThread = new Thread(new Runnable() {

            @Override
            public void run() {
                mLastFrameTime = SystemClock.elapsedRealtime();

                do {
                    postInvalidate();
                    SystemClock.sleep(FRAME_TIME_TARGET);
                    while (mGameState == GAME_PLAYING) {
                        long time = SystemClock.elapsedRealtime();

                        long padding = mLastFrameTime + FRAME_TIME_TARGET - time;
                        padding = (padding > 0) ? padding : 0;
                        SystemClock.sleep(padding);

                        time = SystemClock.elapsedRealtime();
                        update(time - mLastFrameTime);
                        mLastFrameTime = time;
                        postInvalidate();
                    }
                } while (mGameState == GAME_PAUSED);
            }
        });

        initializeGameEnvironment();
        mGameState = GAME_PLAYING;
        mGameThread.start();
    }

    public void initializeGameEnvironment() {
        mBallX = mBallStartX;
        mBallY = mBallStartY;

        mAngraveX = mAngraveStartX;
        mAngraveTargeting = false;

        mScore = 0;
        mGotHighScore = false;

        // downward facing quarter circle angle
        double startAngle = -Math.PI / 4.0 + Math.PI / 2.0 * mRandom.nextDouble();

        mBallVx = mBallStartSpeed * (float) Math.sin(startAngle);
        mBallVy = mBallStartSpeed * (float) Math.cos(startAngle);

        mAngraveV = mBallStartSpeed * ANGRAVE_BALL_SPEED_FRAC;
    }

    public void update(float delta) {

        mBallX += mBallVx * delta;
        mBallY += mBallVy * delta;

        if (mAngraveTargeting) {
            float angraveDelta = mAngraveTargetX - mAngraveX;
            float angraveMoveDistance = mAngraveV * delta;

            if (Math.abs(angraveDelta) < angraveMoveDistance) {
                mAngraveX = mAngraveTargetX;
            } else {
                mAngraveX += Math.signum(angraveDelta) * angraveMoveDistance;
            }
        }

        bounceCheck();
    }

    public void bounceCheck() {

        if (mBallX < mLeftCollisionX) {
            // bounce off the left wall
            mBallVx *= -1;
            mBallX = mLeftCollisionX;
            mPlayer.playBounce();

        } else if (mBallX > mRightCollisionX) {
            // bounce off the right wall
            mBallVx *= -1;
            mBallX = mRightCollisionX;
            mPlayer.playBounce();
        }

        if (mBallY < mTopCollisionY) {
            // bouce off the top wall
            mBallVy *= -1;
            mBallY = mTopCollisionY;
            mPlayer.playBounce();

        } else if (mBallY > mAngraveCollisionHeight && Math.abs(mBallX - mAngraveX) < ANGRAVE_DRAW_OFFSET) {

            float v = BALL_BOUNCE_FACTOR * (float) Math.sqrt(mBallVx * mBallVx + mBallVy * mBallVy);

            // limit max speed
            if (v >= mMaxSpeed) {
                v = mMaxSpeed;
            }

            double angle = Math.atan(mBallVx / mBallVy);
            angle += BOUNCE_ANGLE_RANDOMIZER * mRandom.nextDouble();

            // prevent ball from going too horizontal
            if (angle > MAX_RETURN_ANGLE) {
                angle = MAX_RETURN_ANGLE;
            } else if (angle < -MAX_RETURN_ANGLE) {
                angle = -MAX_RETURN_ANGLE;
            }

            mBallVy = -v * (float) Math.cos(angle);
            mBallVx = +v * (float) Math.sin(angle);
            mBallY = mAngraveCollisionHeight;

            mAngraveV = ANGRAVE_BALL_SPEED_FRAC * v;

            mPlayer.playBounce();
            score();

        } else if (mBallY > mBottomCollisionY) {
            lose();
        }
    }

    public void score() {
        mScore += 1;
    }

    public void lose() {
        mGameState = GAME_LOST;
        mGamesLost++;

        if (mScore > mHighScore) {
            mGotHighScore = true;
            mHighScore = mScore;
        }

    }

    /*
     * Because there is no proper synchronization between the game and UI
     * threads the getInsult method might get called multiple times in a short
     * period. To give a consistent UI experience (and avoid implementing
     * something like dubbel buffering for the complete state) this buffer is
     * used to ensure consistent responses when getting multiple insults in a
     * short period of time.
     */
    private String mInsultBuffer;
    private long   mLastInsultCalled = 0L;

    private String getInsult() {

        if (SystemClock.elapsedRealtime() - mLastInsultCalled < 100L) {
            return mInsultBuffer;
        } else {
            mLastInsultCalled = SystemClock.elapsedRealtime();
        }

        if (mGotHighScore) {
            String newHighScore = "New High Score: " + mHighScore + ", ";
            if (mGamesLost < 5) {
                mInsultBuffer = newHighScore + pickRandom(someInsults);
                return mInsultBuffer;
            } else if (mGamesLost < 10) {
                mInsultBuffer = newHighScore + pickRandom(moreInsults);
                return mInsultBuffer;
            } else {
                mInsultBuffer = newHighScore + pickRandom(allInsults);
                return mInsultBuffer;
            }
        } else {
            String noHighScore = "Only " + mScore + ", ";
            if (mGamesLost < 5) {
                mInsultBuffer = noHighScore + pickRandom(someInsults);
                return mInsultBuffer;
            } else if (mGamesLost < 10) {
                mInsultBuffer = noHighScore + pickRandom(moreInsults);
                return mInsultBuffer;
            } else {
                mInsultBuffer = noHighScore + pickRandom(allInsults);
                return mInsultBuffer;
            }
        }
    }

    private String pickRandom(List<String> s) {
        return s.get(mRandom.nextInt(s.size()));
    }

}
