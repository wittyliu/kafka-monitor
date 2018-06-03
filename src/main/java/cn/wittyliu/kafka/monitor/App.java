package cn.wittyliu.kafka.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.wittyliu.kafka.monitor.core.KafkaMonitorConf;
import cn.wittyliu.kafka.monitor.core.KafkaMonitorTimeoutThread;
import cn.wittyliu.kafka.monitor.util.KafkaUtil;
import cn.wittyliu.kafka.monitor.util.ZookeeperUtil;

public class App {

	private static final Logger logger = LoggerFactory.getLogger(App.class);

	private ZooKeeper zk;
	private KafkaConsumer<String, String> consumer;
	
	public App(KafkaMonitorConf conf) {
		checkAndInit(conf);
	}
	
	public void checkAndInit(KafkaMonitorConf conf) {
		if (this.zk == null || States.CLOSED.equals(this.zk.getState())) {
			this.zk = ZookeeperUtil.getZookeeper(conf.getZkConnString(), conf.getZkTimeout());
			if (this.zk == null) {
				throw new RuntimeException("获取zookeeper连接失败");
			}
		}
		if (this.consumer == null) {
			this.consumer = KafkaUtil.getKafkaConsumer(conf.getGroupId(), conf.getBootstrapServers());
		}
	}

	public void destroy(){
		if(this.zk != null) {
			try {
				this.zk.close();
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
		}
		if(this.consumer != null) {
			this.consumer.close();
		}
	}
	

	public ZooKeeper getZk() {
		return zk;
	}

	public KafkaConsumer<String, String> getConsumer() {
		return consumer;
	}

	
	public static void main(String[] args) throws Exception {

		final KafkaMonitorConf conf = new KafkaMonitorConf();

		if (args != null && args.length > 0 && args[0] != null && args[0].length() > 0) {
			conf.loadConfig(args[0].trim(), true);
		} else {
			conf.loadConfig(KafkaMonitorConf.PROPERTY_NAME, false);
		}
		logger.info("加载的配置：{}", conf.toString());

		final App app = new App(conf);
		
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		//定时器执行的Runable线程不能抛异常 否则会导致定时器停止，程序崩溃
		Thread task = new Thread(new KafkaMonitorTimeoutThread(conf, app));
		try {
			// 上一次任务完成后，固定时间后执行下一次任务
			scheduledExecutorService.scheduleWithFixedDelay(task, 0L, conf.getWarnInterval(), TimeUnit.SECONDS);
		} catch(Throwable e) {
			app.destroy();
		}

	}

}