package com.example.geogram.utils;

import android.os.Environment;

public class FilePath {

    /**"storage/emulator/0"
     * default system
     */
    public String ROOT_DIR = Environment.getExternalStorageDirectory().getPath();
    //public String ROOT_DIR = Environment.getExternalStorageState();

    public String PICTURES = ROOT_DIR + "/Pictures";
    public String CAMERA = ROOT_DIR + "/DCIM/camera";

    public String FIREBASE_IMAGE_STORAGE = "photos/users/";
}
