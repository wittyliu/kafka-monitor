package cn.wittyliu.kafka.monitor.util;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class ZookeeperUtil {

	public static ZooKeeper getZookeeper(String connectionString, int zkTimeout) {
		final CountDownLatch latch = new CountDownLatch(1);
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(connectionString, zkTimeout, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (Event.KeeperState.SyncConnected.equals(event.getState())) {
						latch.countDown();
					}
				}
			});
			latch.await();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return zk;
	}
	
}
