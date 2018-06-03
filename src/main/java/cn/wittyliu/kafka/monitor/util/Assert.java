package cn.wittyliu.kafka.monitor.util;

import java.text.MessageFormat;

public class Assert {

	public static void assertConfigNotEmpty(Object object, String configName) throws IllegalArgumentException {
		if (object != null) {
			if (object instanceof String) {
				String objStr = (String) object;
				if (objStr.trim().length() > 0) {
					return;
				}
			} else {
				return;
			}
		}
		throw new IllegalArgumentException(MessageFormat.format("配置{0}不能为空", configName));
	}
}
