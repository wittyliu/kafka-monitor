package cn.wittyliu.kafka.monitor.util;

import java.util.Properties;

public class StringUtil {

	public static String getTrimString(Properties prop, String key) {
		return getTrimString(prop, key, true);
	}
	public static String getTrimString(Properties prop, String key, boolean canbeEmpty) {
		return getTrimString(prop, key, canbeEmpty, "属性");
	}
	
	public static String getTrimString(Properties prop, String key, boolean canbeEmpty, String emptyExceptionPrefix) {
		String value = prop.getProperty(key);
		if(isNotEmpty(value)) {
			return value.trim();
		}else if(canbeEmpty) {
			return null;
		}else {
			throw new IllegalArgumentException(emptyExceptionPrefix + key + "不能为空");
		}
	}
	
	
	/**
	 * 
	 * @param prop
	 * @param key
	 * @param defaultValue
	 * @return
	 * @throws NumberFormatException	不处理数值转换异常
	 */
	public static int getInt(Properties prop, String key, int defaultValue) throws NumberFormatException{
		String value = getTrimString(prop, key, true);
		if(value != null) {
			return Integer.parseInt(value);
		}else {
			return defaultValue;
		}
	}
	
	public static boolean isEmpty(String str){
		if(str==null || str.trim().length() == 0) {
			return true;
		}
		return false;
	}
	
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}
	
}
