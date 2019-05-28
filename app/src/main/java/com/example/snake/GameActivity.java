package com.example.snake;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

public class GameActivity extends Activity {

    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    String dataName = "myData";
    String intName = "MyInt";
    int defaultInt = 0;
    int hiScore = -1;


    Canvas canvas;
    SnakeView snakeView;

    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;




    //Sound
    //and sound variables
    private SoundPool soundPool;
    int sample1 = -1;
    int sample4 = -1;

    //for snake movement

    int directionOfTravel = 0;

    // 0 is up, 1 is right, 2 down, 3 left.

    int screenWidth;
    int screenHeight;
    int topGap;



    //stats
    long lastFrameTime;
    int fps;
    int score = 0;
    //game objects

    int[] snakeX;
    int[] snakeY;
    int[] snakeH;
    int snakeLength;
    int appleX;
    int appleY;

    //pixel size of a place on gameboard
    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    //head rotation
    Matrix matrix90 = new Matrix();
    Matrix matrix180 = new Matrix();
    Matrix matrix270 = new Matrix();


    Matrix matrixHeadFlip = new Matrix();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(dataName, MODE_PRIVATE);
        editor = prefs.edit();
        hiScore = prefs.getInt(intName, defaultInt);

        loadSound();
        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);

    }


    class SnakeView extends SurfaceView implements Runnable {
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSnake;
        Paint paint;

        public SnakeView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            //snake length

            snakeX = new int[200];
            snakeY = new int[200];
            snakeH = new int[200];
            //making the snake
            getSnake();
            //getting the apple
            getApple();
        }

        public void getSnake() {

            snakeLength = 3;
            //start snake head in a middle of screen
            snakeX[0] = numBlocksWide / 2;
            snakeY[0] = numBlocksHigh / 2;
            //snakes body
            snakeX[1] = snakeX[0] - 1;
            snakeY[1] = snakeY[0];

            //snake tail
            snakeX[1] = snakeX[1] - 1;
            snakeY[1] = snakeY[0];
        }

        public void getApple() {
            Random random = new Random();
            appleX = random.nextInt(numBlocksWide - 1) + 1;
            appleY = random.nextInt(numBlocksHigh - 1) + 1;
        }

        @Override
        public void run() {
            while (playingSnake) {
                updateGame();
                drawGame();
                controlFPS();
            }
        }

        public void updateGame() {

            //checking if player got the apple
            if (snakeX[0] == appleX && snakeY[0] == appleY) {
                //growing snake after apple eating
                snakeLength++;
                //replace apple
                getApple();
                //add score
                score = score + snakeLength;
                soundPool.play(sample1, 1,
                        1, 0, 0, 1);
            }

            //move the body at the back
            for (int i = snakeLength; i > 0; i--) {
                snakeX[i] = snakeX[i - 1];
                snakeY[i] = snakeY[i - 1];

                //change heading
                snakeH[i] = snakeH[i - 1];
            }

            //Moving the head in an appropriate direction
            switch (directionOfTravel) {
                case 0://up
                    snakeY[0]  --;
                    snakeH[0] = 0;
                    break;

                case 1://right
                    snakeX[0] ++;
                    snakeH[0] = 1;
                    break;

                case 2://down
                    snakeY[0] ++;
                    snakeH[0] = 2;
                    break;

                case 3://left
                    snakeX[0] --;
                    snakeH[0] = 3;
                    break;
            }

            //Death

            boolean dead = false;

            //with wall
            if (snakeX[0] == -1) dead = true;
            if (snakeX[0] >= numBlocksWide) dead = true;
            if (snakeY[0] == -1) dead = true;
            if (snakeY[0] == numBlocksHigh) dead = true;
            //if eaten ourselves

            for (int i = snakeLength - 1; i > 0; i--) {
                if ((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i])) {
                    dead = true;
                }
            }

            if (dead) {
                if (score > hiScore) {
                    hiScore = score;
                    editor.putInt(intName, hiScore);
                    editor.commit();
                }



                soundPool.play(sample4, 1,
                        1, 0, 0, 1);

                //restart
                score = 0;
                getSnake();

            }

        }

        public void drawGame() {

            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();

                //Paint paint = new Paint();
                canvas.drawColor(Color.BLACK); //background color
                paint.setColor(Color.GREEN);
                paint.setTextSize(topGap / 2);
                canvas.drawText("My Score: " + score +
                        " High Score" + hiScore, 10, topGap - 10, paint);

                //drawing the border - 4 lines all sides
                paint.setStrokeWidth(3);//4 pixel border
                canvas.drawLine(1,topGap,screenWidth-1,topGap,paint);
                canvas.drawLine(screenWidth-1,topGap,
                        screenWidth-1,topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(screenWidth-1,
                        topGap+(numBlocksHigh*blockSize),1,
                        topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(1,topGap, 1,topGap+
                        (numBlocksHigh*blockSize), paint);

                //drawing the snake
                Bitmap rotatedBitmap;
                Bitmap rotatedTailBitmap;


                rotatedBitmap = headBitmap;
                switch (snakeH[0]) {

                    case 0://up
                        rotatedBitmap = Bitmap.createBitmap(rotatedBitmap, 0, 0,
                                rotatedBitmap.getWidth(), rotatedBitmap.getHeight(),
                                matrix270, true);
                        break;
                    case 1://right
                        //no rotation needed
                        break;

                    case 2://down
                        rotatedBitmap = Bitmap.createBitmap(rotatedBitmap,
                                0, 0, rotatedBitmap.getWidth(),
                                rotatedBitmap.getHeight(), matrix90, true);
                        break;

                    case 3: // left
                        rotatedBitmap = Bitmap.createBitmap(rotatedBitmap,
                                0, 0, rotatedBitmap.getWidth(),
                                rotatedBitmap.getHeight(), matrixHeadFlip, true);
                        break;
                }
                canvas.drawBitmap(rotatedBitmap,
                        snakeX[0] * blockSize,
                        (snakeY[0] * blockSize) + topGap, paint);
                //drawing body
                rotatedBitmap = bodyBitmap;

                for (int i = 1; i < snakeLength - 1; i++) {

                    switch (snakeH[i]) {
                        case 0://up
                            rotatedBitmap = Bitmap.createBitmap(bodyBitmap,
                                    0, 0, bodyBitmap.getWidth(),
                                    bodyBitmap.getHeight(), matrix270, true);
                            break;
                        case 1://right
                            //no rotation needed
                            break;
                        case 2://down
                            rotatedBitmap = Bitmap.createBitmap(bodyBitmap,
                                    0, 0, bodyBitmap.getWidth(),
                                    bodyBitmap.getHeight(), matrix90, true);
                            break;
                        case 3://left
                            rotatedBitmap = Bitmap.createBitmap(bodyBitmap,
                                    0, 0, bodyBitmap.getWidth()
                                    , bodyBitmap.getHeight(), matrix180, true);
                            break;
                    }
                    canvas.drawBitmap(rotatedBitmap, snakeX[i] * blockSize,
                            (snakeY[i] * blockSize) + topGap, paint);
                }


                rotatedTailBitmap = Bitmap.createBitmap(tailBitmap);

                switch (snakeH[snakeLength - 1]) {
                    case 0: //up
                        rotatedTailBitmap = Bitmap.createBitmap(rotatedBitmap,
                                0, 0, rotatedBitmap.getWidth()
                                , rotatedBitmap.getHeight(), matrix270, true);
                        break;
                    case 1://right
                        //no rotation needed
                        break;
                    case 2: //down
                        rotatedTailBitmap = Bitmap.createBitmap(rotatedBitmap,
                                0, 0, rotatedBitmap.getWidth()
                                , rotatedBitmap.getHeight(), matrix90, true);
                        break;
                    case 3://left
                        rotatedTailBitmap = Bitmap.createBitmap(rotatedBitmap,
                                0, 0, rotatedBitmap.getWidth()
                                , rotatedBitmap.getHeight(), matrix180, true);
                        break;


                }
                canvas.drawBitmap(rotatedTailBitmap, snakeX[snakeLength - 1] * blockSize,
                        (snakeY[snakeLength - 1] * blockSize) + topGap, paint);

                //drawing the apple
                canvas.drawBitmap(appleBitmap,
                        appleX * blockSize, (appleY * blockSize) + topGap, paint);
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }


        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 200 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {
                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                }
            }
            lastFrameTime = System.currentTimeMillis();
        }



        public void pause() {
            playingSnake = false;
            try {
                ourThread.join();

            } catch (InterruptedException e) {

            }
        }

        public void resume() {
            playingSnake = true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    if (motionEvent.getX() >= screenWidth / 2) {
                        //turn right
                        directionOfTravel++;
                        if (directionOfTravel == 4) { // no direction
                            //loop up to 0(up)
                            directionOfTravel = 0;
                        }
                    } else {
                        //turn left
                        directionOfTravel--;
                        if (directionOfTravel == -1) { // no dir
                            //loop to 0
                            directionOfTravel = 3;
                        }
                    }


            }
            return true;
        }
    }



    @Override
    protected void onStop() {
        super.onStop();


        snakeView.pause();


        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            snakeView.pause();

            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }

    public void loadSound() {

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            //creating objects for 2 classes
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //create our three fix memory
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundPool.load(descriptor, 0);

        } catch (IOException e) {
            //error msg

        }
    }

    public void configureDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        topGap = screenHeight / 14;

        //Determine the size of each block/place on the board
        blockSize = screenWidth / 30;

        //Determine how many blocks to fill
        //leave on block for the score

        numBlocksWide = 30;
        numBlocksHigh = ((screenHeight - topGap)) / blockSize;

        //load my bitmaps
        loadBitmap();

        //scaling bitmaps to match block size
        scaleBitmap();

        //for the tail
        tailBitmap();

        headRotation();


    }

    private void headRotation() {
        matrix90.postRotate(90);
        matrix180.postRotate(180);
        matrix270.postRotate(270);
        //headflip
        matrixHeadFlip.setScale(-1, 1);
        matrixHeadFlip.postTranslate(headBitmap.getWidth(), 0);
    }

    private void tailBitmap() {
        tailBitmap = BitmapFactory.decodeResource
                (getResources(), R.drawable.tail_sprite_sheet);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
    }

    private void scaleBitmap() {
        headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize,
                blockSize, false);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap, blockSize,
                blockSize, false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize,
                blockSize, false);
    }

    private void loadBitmap() {
        headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.head);
        bodyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.body);
        tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tail);
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple);
    }

}