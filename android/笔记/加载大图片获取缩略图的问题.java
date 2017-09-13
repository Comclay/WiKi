// Android应用加载大图片出现内存溢出(OOM)或者获取视频图片的的缩略图的问题？

/*
第一种是BitmapFactory和BitmapFactory.Options。
首先，BitmapFactory.Options有几个Fields很有用：
	inJustDecodeBounds：If set to true, the decoder will return null (no bitmap), but the out...
也就是说，当inJustDecodeBounds设成true时，bitmap并不加载到内存，这样效率很高哦。
而这时，你可以获得bitmap的高、宽等信息。
	outHeight：The resulting height of the bitmap, set independent of the state of inJustDecodeBounds.
	outWidth：The resulting width of the bitmap, set independent of the state of inJustDecodeBounds. 
看到了吧，上面3个变量是相关联的哦。
	inSampleSize ：  If set to a value > 1, requests the decoder to subsample the original image, 
	returning a smaller image to save memory.
这就是用来做缩放比的。这里有个技巧：
	inSampleSize=（outHeight/Height+outWidth/Width）/2
实践证明，这样缩放出来的图片还是很好的。
最后用BitmapFactory.decodeFile(path, options)生成。
由于只是对bitmap加载到内存一次，所以效率比较高。解析速度快。
*/

/*
第二种是使用Bitmap加Matrix来缩放。
首先要获得原bitmap，再从原bitmap的基础上生成新图片。这样效率很低。
*/

/*
第三种是用2.2新加的类ThumbnailUtils来做。
让我们新看看这个类，从API中来看，此类就三个静态方法：
	createVideoThumbnail、extractThumbnail(Bitmap source, int width, int height, int options)
	、extractThumbnail(Bitmap source, int width, int height)。
我这里使用了第三个方法。再看看它的源码，下面会附上。是上面我们用到的BitmapFactory.Options
和Matrix等经过人家一阵加工而成。效率好像比第二种方法高一点点。
*/

//使用Bitmap加Matrix来缩放  
    public static Drawable resizeImage(Bitmap bitmap, int w, int h)   
    {    
        Bitmap BitmapOrg = bitmap;    
        int width = BitmapOrg.getWidth();    
        int height = BitmapOrg.getHeight();    
        int newWidth = w;    
        int newHeight = h;    
  
        float scaleWidth = ((float) newWidth) / width;    
        float scaleHeight = ((float) newHeight) / height;    
  
        Matrix matrix = new Matrix();    
        matrix.postScale(scaleWidth, scaleHeight);    
        // if you want to rotate the Bitmap     
        // matrix.postRotate(45);     
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,    
                        height, matrix, true);    
        return new BitmapDrawable(resizedBitmap);    
    }  
      
    //使用BitmapFactory.Options的inSampleSize参数来缩放  
    public static Drawable resizeImage2(String path,  
            int width,int height)   
    {  
        BitmapFactory.Options options = new BitmapFactory.Options();  
        options.inJustDecodeBounds = true;//不加载bitmap到内存中  
        BitmapFactory.decodeFile(path,options);   
        int outWidth = options.outWidth;  
        int outHeight = options.outHeight;  
        options.inDither = false;  
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;  
        options.inSampleSize = 1;  
          
        if (outWidth != 0 && outHeight != 0 && width != 0 && height != 0)   
        {  
            int sampleSize=(outWidth/width+outHeight/height)/2;  
            Log.d(tag, "sampleSize = " + sampleSize);  
            options.inSampleSize = sampleSize;  
        }  
      
        options.inJustDecodeBounds = false;  
        return new BitmapDrawable(BitmapFactory.decodeFile(path, options));       
    }  
  
    //图片保存  
    private void saveThePicture(Bitmap bitmap)  
    {  
        File file=new File("/sdcard/2.jpeg");  
        try  
        {  
            FileOutputStream fos=new FileOutputStream(file);  
            if(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos))  
            {  
                fos.flush();  
                fos.close();  
            }  
        }  
        catch(FileNotFoundException e1)  
        {  
            e1.printStackTrace();  
        }  
        catch(IOException e2)  
        {  
            e2.printStackTrace();  
        }  
    }  
}  