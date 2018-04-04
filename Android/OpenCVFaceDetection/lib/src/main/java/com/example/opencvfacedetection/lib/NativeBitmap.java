package com.example.opencvfacedetection.lib;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by aoi on 2018/03/31.
 */

public class NativeBitmap {

    private Plane mPlane;

    public NativeBitmap() {
    }

    public int createTexture(Bitmap bitmap) {
        mPlane = new Plane(bitmap);
        return mPlane.getTextureId();
    }

    public Plane getPlane() {
        return mPlane;
    }
}
