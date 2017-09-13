// 获取手机sd卡的路径


/*
	1,通过系统的StroageManager来获取
		由于 getVolumePaths() 方法为一个hide的方法，所以用反射的方式来获取手机的sd卡路径
*/

	private StorageManager mStorageManager;
    private Method mMethodGetPaths;

    public String[] getVolumePaths() {
        mStorageManager = (StorageManager) this
                .getSystemService(Context.STORAGE_SERVICE);
        try {
            mMethodGetPaths = mStorageManager.getClass()
                    .getMethod("getVolumePaths");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        String[] paths = null;
        try {
            paths = (String[]) mMethodGetPaths.invoke(mStorageManager);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        Log.i(TAG,paths.toString());
        return paths;
    }
	
/*第2种方式*/

/**
     * 遍历 "system/etc/vold.fstab” 文件，获取全部的Android的挂载点信息
     *
     * @return
     */
    private static ArrayList<String> getDevMountList() {
        String[] toSearch = FileUtil.readFile("/system/etc/vold.fstab").split(" ");
        ArrayList<String> out = new ArrayList<String>();
        for (int i = 0; i < toSearch.length; i++) {
            if (toSearch[i].contains("dev_mount")) {
                if (new File(toSearch[i + 2]).exists()) {
                    out.add(toSearch[i + 2]);
                }
            }
        }

        Log.i(TAG,out.toString());
        return out;
    }

    /**
     * 获取扩展SD卡存储目录
     *
     * 如果有外接的SD卡，并且已挂载，则返回这个外置SD卡目录
     * 否则：返回内置SD卡目录
     *
     * @return
     */
    public String getExternalSdCardPath() {
//        String defaultSdPath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File sdCardFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
//            defaultSdPath = sdCardFile.getAbsolutePath();
//            addRootItem(getString(R.string.root_file_sd), R.drawable.file_icon_folder,defaultSdPath);
            return sdCardFile.getAbsolutePath();
        }

        String path = null;

        File sdCardFile = null;

        ArrayList<String> devMountList = getDevMountList();

        for (String devMount : devMountList) {
//            if (devMount.equals(defaultSdPath)){
//                continue;
//            }
            File file = new File(devMount);

            if (file.isDirectory() && file.canWrite()) {
                path = file.getAbsolutePath();

                String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
                File testWritable = new File(path, "test_" + timeStamp);

                if (testWritable.mkdirs()) {
                    testWritable.delete();
                } else {
                    path = null;
                }
            }
        }

        if (path != null) {
            sdCardFile = new File(path);

            return sdCardFile.getAbsolutePath();
        }

        return null;
    }