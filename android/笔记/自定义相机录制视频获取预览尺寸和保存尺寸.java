private Size findBestPictureSize(Camera.Parameters parameters) {
        int diff = Integer.MIN_VALUE;
        String pictureSizeValueString = parameters.get("picture-size-values");
        // saw this on Xperia
        if (pictureSizeValueString == null) {
            pictureSizeValueString = parameters.get("picture-size-value");
        }
        if (pictureSizeValueString == null) {
            return mCamera.new Size(getScreenWH().widthPixels, getScreenWH().heightPixels);
        }
        int bestX = 0;
        int bestY = 0;
        for (String pictureSizeString : Camera.COMMA_PATTERN.split(pictureSizeValueString)) {
            pictureSizeString = pictureSizeString.trim();
            int dimPosition = pictureSizeString.indexOf('x');
            if (dimPosition == -1) {
                continue;
            }
            int newX = 0;
            int newY = 0;
            try {
                newX = Integer.parseInt(pictureSizeString.substring(0, dimPosition));
                newY = Integer.parseInt(pictureSizeString.substring(dimPosition + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            Point screenResolution = new Point(getScreenWH().widthPixels, getScreenWH().heightPixels);
            int newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y);
            if (newDiff == diff) {
                bestX = newX;
                bestY = newY;
                break;
            } else if (newDiff > diff) {
                if ((3 * newX) == (4 * newY)) {
                    bestX = newX;
                    bestY = newY;
                    diff = newDiff;
                }
            }
        }
        if (bestX > 0 && bestY > 0) {
            return mCamera.new Size(bestX, bestY);
        }
        return null;
    }


    private Size findBestPreviewSize(Camera.Parameters parameters) {
        String previewSizeValueString = null;
        int diff = Integer.MAX_VALUE;
        previewSizeValueString = parameters.get("preview-size-values");
        if (previewSizeValueString == null) {
            previewSizeValueString = parameters.get("preview-size-value");
        }
        if (previewSizeValueString == null) {
            // 有些手机例如m9获取不到支持的预览大小   就直接返回屏幕大小
            return mCamera.new Size(getScreenWH().widthPixels, getScreenWH().heightPixels);
        }
        int bestX = 0;
        int bestY = 0;
        for (String prewsizeString : COMMA_PATTERN.split(previewSizeValueString)) {
            prewsizeString = prewsizeString.trim();
            int dimPosition = prewsizeString.indexOf('x');
            if (dimPosition == -1) {
                continue;
            }
            int newX = 0;
            int newY = 0;
            try {
                newX = Integer.parseInt(prewsizeString.substring(0, dimPosition));
                newY = Integer.parseInt(prewsizeString.substring(dimPosition + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            Point screenResolution = new Point(getScreenWH().widthPixels, getScreenWH().heightPixels);
            int newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y);
            if (newDiff == diff) {
                bestX = newX;
                bestY = newY;
                break;
            } else if (newDiff < diff) {
                if ((3 * newX) == (4 * newY)) {
                    bestX = newX;
                    bestY = newY;
                    diff = newDiff;
                }
            }
        }
        if (bestX > 0 && bestY > 0) {
            return mCamera.new Size(bestX, bestY);
        }
        return null;
    }