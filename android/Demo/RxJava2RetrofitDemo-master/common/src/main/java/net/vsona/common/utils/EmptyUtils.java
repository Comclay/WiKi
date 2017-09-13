package net.vsona.common.utils;

/**
 * 作者 : zhoukang
 * 日期 : 2017-06-17  12:38
 * 说明 : 判空工具类
 */

public class EmptyUtils {
    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(CharSequence str){
        return str == null || str.toString().trim().isEmpty();
    }

    /**
     * 判断字符串不为空
     */
    public static boolean isNotEmpty(CharSequence str){
        return !isEmpty(str);
    }
}
