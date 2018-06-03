package cn.wittyliu.kafka.monitor.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.wittyliu.kafka.monitor.App;
import cn.wittyliu.kafka.monitor.handler.WarnExceptionHandler;

public class KafkaMonitorTimeoutThread implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(KafkaMonitorTimeoutThread.class);

	private ExecutorService executorService = Executors.newFixedThreadPool(4);
	private App app;
	private KafkaMonitorConf conf;

	public KafkaMonitorTimeoutThread(KafkaMonitorConf monitorConf, App app) {
		this.conf = monitorConf;
		this.app = app;
	}

	// 不能抛出异常，否则会崩溃
	@Override
	public void run() {
		boolean hasException = false;
		Future<Boolean> future = null;
		try {
			KafkaMonitorTask monitorTask = new KafkaMonitorTask(conf, app);
			future = executorService.submit(monitorTask);
			future.get(conf.getWarnTimeout(), TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			hasException = true;
			logger.warn("告警检测任务执行异常", e);
		} catch (Exception e) {
			if (future != null && !future.isCancelled()) {
				try {
					future.cancel(true);
				} catch (Exception e1) {
				}
			}
			hasException = true;
			logger.warn("告警检测任务执行超时" + conf.getWarnTimeout() + "s，中断一次kafka告警检测任务", e);
		}

		if (hasException) {
			int leftWarnExceptionTimes = this.conf.getLeftWarnExceptionTimes().decrementAndGet();
			if (leftWarnExceptionTimes <= 0) {
				try {
					logger.warn("告警检测任务连续{}次执行失败, 尝试执行对应处理任务：{}", conf.getWarnExceptionTimes(),
							conf.getWarnExceptionHandlerType().getCaption());
					WarnExceptionHandler.handle(app, conf);
				} catch (Exception e1) {
					logger.warn("对应处理任务执行异常", e1);
				}
				// handler没有关闭程序，重新开始计数
				this.conf.getLeftWarnExceptionTimes().set(this.conf.getWarnExceptionTimes());
			}
		} else {
			// 执行成功 重新开始计数
			this.conf.getLeftWarnExceptionTimes().set(this.conf.getWarnExceptionTimes());
		}

	}

}
