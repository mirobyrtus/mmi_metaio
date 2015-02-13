package com.metaio.Example;

import android.content.Context;
import android.util.Log;

import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class MetaioMan {

    IGeometry model;
    Vector3d translation;

    private static Random rand = new Random();

    private static String METAIOMAN_MODEL_PATH = "TutorialTrackingSamples/Assets/metaioman.md2";
    // private static String METAIOMAN_MODEL_PATH = "TutorialTrackingSamples/Assets/batman.md2";

    public MetaioMan(IGeometry geometry) {
        model = geometry;
        initTranslation();
    }

    public void enable(int coordinateSystemId) {
        // model.setCoordinateSystemID(coordinateSystemId);
        model.setVisible(true);
    }

    public void initTranslation() {
        int randomX = rand.nextInt(300);
        int randomY = rand.nextInt(300);
        translation = new Vector3d(200f + randomX, 200f + randomY, 0f);
    }

    public boolean updateTranslation() {
        if (translation.getX() > 0) {
            translation.setX(translation.getX() - 1);
        }

        if (translation.getY() > 0) {
            translation.setY(translation.getY() - 1);
        }

        if (translation.getZ() > 0) {
            translation.setZ(translation.getZ() - 1);
        }

        if (translation.getX() == 0 && translation.getY() == 0 && translation.getZ() == 0) {
            return true;
        }

        return false;
    }

    public void translate(Vector3d tigerTranslation) {
        model.setTranslation(new Vector3d(
                tigerTranslation.getX() + translation.getX(),
                tigerTranslation.getY() + translation.getY(),
                tigerTranslation.getZ() + translation.getZ()
        ));
    }

    public static ArrayList<MetaioMan> loadModel(Context context, IMetaioSDKAndroid metaioSDK, int count) {
        if (count > 10) {
            Log.e("InitMetaioMen", "Max count is 10!");
        }

        ArrayList<MetaioMan> result = new ArrayList<MetaioMan>(); // TODO (count)

        // Load metaioMan
        final File modelFile = AssetsManager.getAssetPathAsFile(context, METAIOMAN_MODEL_PATH);

        for (int metaioManId = 0; metaioManId < count; metaioManId++) {
            IGeometry model = metaioSDK.createGeometry(modelFile);
            model.setName("MetaioMan" + metaioManId);
            model.setScale(0.5f);

            result.add(new MetaioMan(model));
        }

        return result;
    }


}
