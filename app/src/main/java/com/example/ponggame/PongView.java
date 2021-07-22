package com.example.ponggame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.*;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.ponggame.Ball;
import com.example.ponggame.Bat;

import java.io.IOException;

public class PongView extends SurfaceView implements Runnable {

    // This is our thread
    Thread mGameThread = null;

    // Bandavya
    SurfaceHolder mOurHolder;

    // Bandavya A boolean which we will set and unset
    // when the game is running- or not
    // It is volatile because it is accessed from inside and outside the thread
    volatile boolean mPlaying;

    // Game is mPaused at the start
    boolean mPaused = true;


    // A Canvas and a Paint object
    Canvas mCanvas;
    Paint mPaint;

    // This variable tracks the game frame rate
    long mFPS;

    // The size of the screen in pixels
    int mScreenX;
    int mScreenY;

    // The players myBat
    //int x,y;
    //Bat myBat=new Bat(x,y);
    Bat myBat;
    // A myBall
    Ball myBall;

    // For sound FX
    SoundPool sp;
    int beep1ID = -1;
    int beep2ID = -1;
    int beep3ID = -1;
    int loseLifeID = -1;

    // The mScore
    int mScore = 0;

    // Lives
    int mLives = 3;

    /* Bandavya
    When the we call new() on pongView
    This custom constructor runs
*/

    public PongView(Context context, int x, int y) {

    /* Bandavya
        The next line of code asks the
        SurfaceView class to set up our object.
    */
        super(context);

        // Set the screen width and height
        mScreenX = x;
        mScreenY = y;

        // Initialize mOurHolder and mPaint objects
        mOurHolder = getHolder();
        mPaint = new Paint();

        // A new myBat
        myBat = new Bat(mScreenX, mScreenY);

        // Create a myBall
        myBall = new Ball(mScreenX, mScreenY);

    /*
        Instantiate our sound pool
        dependent upon which version
        of Android is present
    */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            sp = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();

        } else {
            sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }


        try{
            // Create objects of the 2 required classes
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Load our fx in memory ready for use
            descriptor = assetManager.openFd("beep1.ogg");
            beep1ID = sp.load(descriptor, 0);

            descriptor = assetManager.openFd("beep2.ogg");
            beep2ID = sp.load(descriptor, 0);

            descriptor = assetManager.openFd("beep3.ogg");
            beep3ID = sp.load(descriptor, 0);

            descriptor = assetManager.openFd("loseLife.ogg");
            loseLifeID = sp.load(descriptor, 0);

            //descriptor = assetManager.openFd("explode.ogg");
            //explodeID = sp.load(descriptor, 0);

        }catch(IOException e){
            // Print an error message to the console
            Log.e("error", "failed to load sound files");
        }

        setupAndRestart();

    }

    public void setupAndRestart(){

        // Put the myBall back to the start
        myBall.reset(mScreenX, mScreenY);

        // if game over reset scores and mLives
        if(mLives == 0) {
            mScore = 0;
            mLives = 3;
        }

    }

    //Code the overridden run method.
    @Override
    public void run() {
        while (mPlaying) {

            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();

            // Update the frame
            // Update the frame
            if(!mPaused){
                update();
            }

            // Draw the frame
            draw();

        /*
            Calculate the FPS this frame
            We can then use the result to
            time animations in the update methods.
        */
            long timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                mFPS = 1000 / timeThisFrame;
            }

        }

    }

    // Everything that needs to be updated goes in here
// Movement, collision detection etc.
    public void update() {

        // Move the myBat if required
        myBat.update(mFPS);

        myBall.update(mFPS);

        //RectF BallRectf = myBall.getRect();
        //RectF BatRectf = myBat.getRect();


        //if(RectF.intersects(BatRectf, BallRectf)){
        if (RectF.intersects(myBat.getRect(), myBall.getRect())) {
            myBall.setRandomXVelocity();
            myBall.reverseYVelocity();
            myBall.clearObstacleY(myBat.getRect().top - 2);

            mScore++;
            myBall.increaseVelocity();

            sp.play(beep1ID, 1, 1, 0, 0, 1);
        }

        // Bounce the myBall back when it hits the bottom of screen


        //if(BallRectf.bottom > mScreenY) {
        if (myBall.getRect().bottom > mScreenY) {
            myBall.reverseYVelocity();
            myBall.clearObstacleY(mScreenY - 2);

            // Lose a life
            mLives--;
            sp.play(loseLifeID, 1, 1, 0, 0, 1);

            if(mLives == 0){
                mPaused = true;
                setupAndRestart();
            }
        }

        // Bounce the myBall back when it hits the top of screen
        if(myBall.getRect().top < 0){
            myBall.reverseYVelocity();
            myBall.clearObstacleY(12);

            sp.play(beep2ID, 1, 1, 0, 0, 1);
        }

        // If the myBall hits left wall bounce
        if (myBall.getRect().left < 0) {
            myBall.reverseXVelocity();
            myBall.clearObstacleX(2);

            sp.play(beep3ID, 1, 1, 0, 0, 1);
        }

        // If the myBall hits right wall bounce
        if (myBall.getRect().right > mScreenX) {
            myBall.reverseXVelocity();
            myBall.clearObstacleX(mScreenX - 22);

            sp.play(beep3ID, 1, 1, 0, 0, 1);
        }

    }

    // Check for myBall colliding with myBat

    /*
    RectF canvasRecF = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        RectF spriteRecF = getRectF();
        if (!RectF.intersects(canvasRecF, spriteRecF)) {
        destroy();
    }
    */



    // Draw the newly updated scene
    public void draw() {

        // Make sure our drawing surface is valid or we crash
        if (mOurHolder.getSurface().isValid()) {

            // Draw everything here

            // Lock the mCanvas ready to draw
            mCanvas = mOurHolder.lockCanvas();

            // Clear the screen with my favorite color
            mCanvas.drawColor(Color.argb(255, 120, 197, 87));

            // Choose the brush color for drawing
            mPaint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the myBat
            mCanvas.drawRect(myBat.getRect(), mPaint);

            // Draw the myBall
            mCanvas.drawRect(myBall.getRect(), mPaint);


            // Change the drawing color to white
            mPaint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the mScore
            mPaint.setTextSize(40);
            mCanvas.drawText("Score: " + mScore + "   Lives: " + mLives, 10, 50, mPaint);

            // Draw everything to the screen
            mOurHolder.unlockCanvasAndPost(mCanvas);
        }

    }

    // If the Activity is paused/stopped
// shutdown our thread.
    public void pause() {
        mPlaying = false;
        try {
            mGameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }

    }

    // If the Activity starts/restarts
// start our thread.
    public void resume() {
        mPlaying = true;
        mGameThread = new Thread(this);
        mGameThread.start();
    }

    // The SurfaceView class implements onTouchListener
// So we can override this method and detect screen touches.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            // Player has touched the screen
            case MotionEvent.ACTION_DOWN:

                mPaused = false;

                // Is the touch on the right or left?
                if(motionEvent.getX() > mScreenX / 2){
                    myBat.setMovementState(myBat.RIGHT);
                }
                else{
                    myBat.setMovementState(myBat.LEFT);
                }

                break;

            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:

                myBat.setMovementState(myBat.STOPPED);
                break;
        }
        return true;
    }

}