package cn.wittyliu.kafka.monitor.util;

import java.io.IOException;
import java.util.List;

import org.xml.sax.SAXException;

import com.sinovatech.ntf.send.NtfplatService;

public class SmsSendUtil {

	public static void sendMessageToOne(String bizCode, String bizName, String taskName, String content, String target) throws IOException, SAXException {
		NtfplatService.sendSingle(bizCode, bizName, taskName, content, target);
	}
	
	public static void sendMessage(String bizCode, String bizName, String taskName, String content, List<String> targetList) throws IOException, SAXException {
		if(targetList == null || targetList.isEmpty()) {
			throw new IllegalArgumentException("参数targetList不能为空");
		}
		for(String target: targetList) {
			sendMessageToOne(bizCode, bizName, taskName, content, target);
		}
	}
	
}
