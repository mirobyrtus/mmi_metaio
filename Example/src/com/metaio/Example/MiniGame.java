package com.metaio.Example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.tools.SystemInfo;
import com.metaio.tools.io.AssetsManager;

import java.io.File;
import java.util.ArrayList;

import jp.epson.moverio.bt200.DisplayControl;

public class MiniGame extends ARViewActivity {

    /**
     * metaio SDK callback handler
     */
    private MetaioSDKCallbackHandler mCallbackHandler;

    /**
     * Tiger geometry
     */
    private IGeometry mTiger;

    /**
     * Reference to loaded evil metaiomen
     */
    private ArrayList<MetaioMan> mMetaioMen;

    /**
     * Currently loaded tracking configuration file
     */
    File trackingConfigFile;

    /**
     * Flag to indicate proximity to the tiger
     */
    boolean mIsCloseToTiger;

    /**
     * The flag indicating a mode of instant tracking
     *
     * @see {@link com.metaio.sdk.jni.IMetaioSDKAndroid#startInstantTracking(String, String, boolean)}
     */
    boolean mPreview = true;

    /**
     * Whether to set tracking configuration on onInstantTrackingEvent
     */
    boolean mMustUseInstantTrackingEvent = false;

    boolean gameStarted = false;

    public Activity a;
    PointsCounter points;
    LevelManager levelManager = new LevelManager();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mTiger = null;
        mCallbackHandler = new MetaioSDKCallbackHandler();
        mIsCloseToTiger = false;

        a = this;

        points = new PointsCounter(this, (TextView)mGUIView.findViewById(R.id.timeTextView));

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mCallbackHandler.delete();
        mCallbackHandler = null;
    }

    /**
     * This method is regularly called in the rendering loop. It calculates the distance between
     * device and the target and performs actions based on the proximity
     */
    private void checkDistanceToTarget()
    {
        // get tracking values for COS 1
        TrackingValues tv = metaioSDK.getTrackingValues(1);

        // Note, you can use this mechanism also to detect if something is tracking or not.
        // (e.g. for triggering an action as soon as some target is visible on screen)
        if (tv.isTrackingState())
        {
            // calculate the distance as sqrt( x^2 + y^2 + z^2 )
            final float distance = tv.getTranslation().norm();

            // define a threshold distance
            final float threshold = 200;

            // moved close to the tiger
            if (distance < threshold)
            {
                // if not already close to the model
                if (!mIsCloseToTiger)
                {
                    MetaioDebug.log("Moved close to the tiger");
                    mIsCloseToTiger = true;
                    mTiger.startAnimation("tap");
                }
            }
            else
            {
                if (mIsCloseToTiger)
                {
                    MetaioDebug.log("Moved away from the tiger");
                    mIsCloseToTiger = false;
                }
            }
        }
    }

    public void updateMetaioMen() {
        // get tracking values for COS 1
        TrackingValues tv = metaioSDK.getTrackingValues(1);

        // Note, you can use this mechanism also to detect if something is tracking or not.
        // (e.g. for triggering an action as soon as some target is visible on screen)
        if (tv.isTrackingState()) {

            if (! points.running) {
                points.initThread();
            }
            int level = levelManager.getLevel();

            boolean gameOver = false;

            if (mMetaioMen != null) {
                for (MetaioMan metaioMan : mMetaioMen) {
                    metaioMan.translate(mTiger.getTranslation());

                    if (metaioMan.updateTranslation(level)) {
                        gameOver = true;
                        break;
                    }
                }
            }

            if (gameOver) {

                gameStarted = false;
                points.stopThread();
                metaioSDK.stopCamera();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(MiniGame.this)
                                .setTitle("Game Over!")
                                .setMessage("Wanna play again?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        // Start new game!
                                        points.initThread();
                                        levelManager.reset();

                                        for (MetaioMan metaioMan : mMetaioMen) {
                                            metaioMan.initTranslation();
                                        }

                                        metaioSDK.startCamera();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        a.finish();
                                        System.exit(0);

                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();

                    }
                });
            }
        }
    }

    @Override
    protected int getGUILayout()
    {
        return R.layout.mmi_game;
    }

    @Override
    protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
    {
        return mCallbackHandler;
    }

    @Override
    public void onDrawFrame()
    {
        super.onDrawFrame();

        if (gameStarted) {
            checkDistanceToTarget();

            updateMetaioMen();
        }
    }

    public void onButtonClick(View v)
    {
        finish();
    }

    public void onStartGameButtonClicked(View v) {

        if (((RadioButton)findViewById(R.id.tracking3D)).isChecked()) {

            mMustUseInstantTrackingEvent = false;
            mTiger.setVisible(false);
            metaioSDK.startInstantTracking("INSTANT_3D");

            // metaioSDK.startInstantTracking("INSTANT_2D", new File(""), mPreview);
            // metaioSDK.startInstantTracking("INSTANT_2D_GRAVITY", new File(""), mPreview);
            // metaioSDK.startInstantTracking("INSTANT_2D_GRAVITY_SLAM", new File(""), mPreview);
            // metaioSDK.startInstantTracking("INSTANT_2D_GRAVITY_SLAM_EXTRAPOLATED", new File(""), mPreview);
            // mPreview = !mPreview;

        } else {

            trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialTrackingSamples/Assets/TrackingData_MarkerlessFast.xml");
            metaioSDK.setTrackingConfiguration(trackingConfigFile);

        }

        gameStarted = true;
    }

    @Override
    protected void loadContents()
    {
        try
        {
            // Load tiger model
            final File tigerModelPath =
                    AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialInstantTracking/Assets/tiger.md2");
            mTiger = metaioSDK.createGeometry(tigerModelPath);
            mTiger.setName("Tiger");

            // Set geometry properties and initially hide it
            mTiger.setScale(8f);
            mTiger.setRotation(new Rotation(0f, 0f, (float)Math.PI));
            mTiger.setVisible(false);
            mTiger.setAnimationSpeed(60f);
            mTiger.startAnimation("meow");
            MetaioDebug.log("Loaded geometry " + tigerModelPath);

            mMetaioMen = MetaioMan.loadModel(getApplicationContext(), metaioSDK, 3);
            MetaioDebug.log("Loaded MetaioMen");

            // Config File for picture tracking
            // Load the desired tracking configuration
            trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialTrackingSamples/Assets/TrackingData_MarkerlessFast.xml");
            final boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile);
            MetaioDebug.log("Tracking configuration loaded: " + result);


            // Setup seetrough
            File calibrationFile = AssetsManager.getAssetPathAsFile(this, "myCalibration.xml");
            if ((calibrationFile == null ||
                    !metaioSDK.setHandEyeCalibrationFromFile(calibrationFile))
                    && !metaioSDK.setHandEyeCalibrationFromFile())
            {
                metaioSDK.setHandEyeCalibrationByDevice();
            }

            metaioSDK.setStereoRendering(true);
            metaioSDK.setSeeThrough(true);
        }
        catch (Exception e)
        {
            MetaioDebug.log(Log.ERROR, "Error loading geometry: " + e.getMessage());
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();


        if (SystemInfo.isEPSONMoverio())
        {
            new DisplayControl(this).setMode(DisplayControl.DISPLAY_MODE_3D, false);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if (SystemInfo.isEPSONMoverio())
        {
            new DisplayControl(this).setMode(DisplayControl.DISPLAY_MODE_2D, false);
        }
    }

    @Override
    protected void onGeometryTouched(IGeometry geometry)
    {
        String name = geometry.getName();
        if (name.contains("MetaioMan")) {
            String metaioManIdStr = name.substring(name.length() - 1);
            int metaioManId = Integer.parseInt(metaioManIdStr);
            MetaioMan metaioMan = mMetaioMen.get(metaioManId);
            metaioMan.initTranslation();

        } else if (name.equals("Tiger")) {
            geometry.startAnimation("tap");
        }
    }

    final class MetaioSDKCallbackHandler extends IMetaioSDKCallback
    {

        @Override
        public void onSDKReady()
        {
            // show GUI
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mGUIView.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onInstantTrackingEvent(boolean success, File filePath)
        {
            if (success)
            {
                // Since SDK 6.0, INSTANT_3D doesn't create a new tracking configuration anymore
                // (see changelog)
                if (mMustUseInstantTrackingEvent)
                {
                    MetaioDebug.log("MetaioSDKCallbackHandler.onInstantTrackingEvent: " + filePath.getPath());
                    metaioSDK.setTrackingConfiguration(filePath);
                }

                mTiger.setVisible(true);

                for (MetaioMan metaioMan : mMetaioMen) {
                    metaioMan.enable(mTiger.getCoordinateSystemID());
                }

            }
            else
            {
                MetaioDebug.log(Log.ERROR, "Failed to create instant tracking configuration!");
            }
        }

        @Override
        public void onTrackingEvent(TrackingValuesVector trackingValues)
        {
            if (gameStarted) {
                // if we detect any target, we bind the loaded geometry to this target
                if (mTiger != null) {
                    for (int i = 0; i < trackingValues.size(); i++) {
                        final TrackingValues tv = trackingValues.get(i);
                        if (tv.isTrackingState()) {
                            mTiger.setCoordinateSystemID(tv.getCoordinateSystemID());

                            mTiger.setVisible(true);

                            for (MetaioMan metaioMan : mMetaioMen) {
                                metaioMan.enable(mTiger.getCoordinateSystemID());
                            }

                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onAnimationEnd(IGeometry geometry, String animationName)
        {
            // Play a random animation from the list
            final String[] animations = {"meow", "scratch", "look", "shake", "clean"};
            final int random = (int)(Math.random() * animations.length);
            geometry.startAnimation(animations[random]);
        }
    }

}
