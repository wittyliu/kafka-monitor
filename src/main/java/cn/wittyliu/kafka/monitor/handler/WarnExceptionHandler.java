package cn.wittyliu.kafka.monitor.handler;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.wittyliu.kafka.monitor.App;
import cn.wittyliu.kafka.monitor.core.KafkaMonitorConf;
import cn.wittyliu.kafka.monitor.util.SmsSendUtil;

public class WarnExceptionHandler {
	private static Logger logger = LoggerFactory.getLogger(WarnExceptionHandler.class);

	private static String CONTENT_TEMPLATE = "【kafka告警】 监控检测程序已连续失败{0}次{1},请尽快处理";

	public static void handle(App app, KafkaMonitorConf conf) throws Exception {
		if (WarnExceptionHandlerType.WARN.equals(conf.getWarnExceptionHandlerType())) {
			String content = MessageFormat.format(CONTENT_TEMPLATE, conf.getWarnExceptionTimes());
			logger.info("尝试发送告警短信: content={} target={}", content, conf.getMsgTarget());
			SmsSendUtil.sendMessage(conf.getMsgBizCode(), conf.getMsgBizName(), conf.getMsgTaskName(), content,
					conf.getMsgTarget());
		} else if (WarnExceptionHandlerType.EXIT.equals(conf.getWarnExceptionHandlerType())) {
			app.destroy();
			System.exit(1);
		} else if (WarnExceptionHandlerType.WARN_TRY_EXIT.equals(conf.getWarnExceptionHandlerType())) {
			String content = MessageFormat.format(CONTENT_TEMPLATE, conf.getWarnExceptionTimes(), ",已停止运行");
			logger.info("尝试发送告警短信并退出: content={} target={}", content, conf.getMsgTarget());
			SmsSendUtil.sendMessage(conf.getMsgBizCode(), conf.getMsgBizName(), conf.getMsgTaskName(), content,
					conf.getMsgTarget());
			app.destroy();
			System.exit(1);
		}
	}

}
