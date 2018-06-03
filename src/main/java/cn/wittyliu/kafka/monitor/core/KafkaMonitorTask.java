package cn.wittyliu.kafka.monitor.core;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.wittyliu.kafka.monitor.App;
import cn.wittyliu.kafka.monitor.util.SmsSendUtil;


public class KafkaMonitorTask implements Callable<Boolean> {
	private static Logger logger = LoggerFactory.getLogger(KafkaMonitorTask.class);
	private DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private App app;
	private KafkaMonitorConf conf;

	public KafkaMonitorTask(KafkaMonitorConf monitorConf, App app) {
		this.conf = monitorConf;
		this.app = app;
	}

	@Override
	public Boolean call() throws Exception {
		logger.info(">>>> 开始一次kafka lag告警检测");
		execWarnLagCheck(this.conf, this.app);
		logger.info("<<<< 完成一次kafka lag告警检测");
		return Boolean.TRUE;
	}

	public void execWarnLagCheck(KafkaMonitorConf monitorConf, App app) throws Exception {
		String groupId = monitorConf.getGroupId();
		ZooKeeper zk = app.getZk();
		KafkaConsumer<String, String> consumer = app.getConsumer();

		// 获取group消费过的所有topic
		logger.info("从zookeeper获取topics offset：zkState: {}, groupId:{}", zk.getState(), groupId);
		List<String> queryTopicList = zk.getChildren("/consumers/" + groupId + "/offsets", false);

		if (queryTopicList == null || queryTopicList.size() == 0) {
			// logger.warn("从zookeeper获取topics offset为空：zkState: {}, groupId:{}",
			// zk.getState(), groupId);
			throw new Exception(MessageFormat.format("从zookeeper获取topics offset为空：zkState: {0}, groupId:{1}",
					zk.getState(), groupId));
		}

		// 匹配正则的topics列表
		List<String> matchesTopicList = new ArrayList<>();
		// 未匹配正则的topics列表
		List<String> unMatchesTopicList = new ArrayList<>();
		if (monitorConf.getTopics() != null && monitorConf.getTopics().size() > 0) {
			for (String queryTopic : queryTopicList) {
				boolean match = false;
				for (Pattern topicPatt : monitorConf.getTopics()) {
					if (topicPatt.matcher(queryTopic).matches()) {
						match = true;
						break;
					}
				}
				if (match) {
					matchesTopicList.add(queryTopic);
				} else {
					unMatchesTopicList.add(queryTopic);
				}
			}
			logger.info("配置kafka.topics.pattern:{} 匹配到的topics:{}", monitorConf.getTopicPattStr(), matchesTopicList);
			logger.info("配置kafka.topics.pattern:{} 未匹配到的topics:{}", monitorConf.getTopicPattStr(), unMatchesTopicList);
		} else {
			// 未配置kafka.topics.pattern 匹配到所有topic
			matchesTopicList = queryTopicList;
			logger.info("未配置kafka.topics.pattern 匹配所有查询到的topics");
		}

		// 没有匹配到topic
		if (matchesTopicList.size() == 0) {
			// TODO
			return;
		}

		// 超出阈值的topic列表 只取其中有限个
		// List<String>lagOverLimitTopics=new
		// ArrayList<>(monitorConf.getMsgTopicsShowMax());

		// 保存超出阈值的topic, lag列表
		List<Map<String, Object>> overLimitTopicLagMapList = new ArrayList<>();

		// 查询topic partitions
		for (String topic : matchesTopicList) {
			List<PartitionInfo> partitionsFor = consumer.partitionsFor(topic);
			// 由于有时延， 尽量逐个topic查询， 减少lag为负数的情况
			List<TopicPartition> topicPartitions = new ArrayList<>();

			long lagSum = 0L;

			// 获取topic对应的 TopicPartition
			for (PartitionInfo partitionInfo : partitionsFor) {
				TopicPartition topicPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
				topicPartitions.add(topicPartition);
			}

			// 查询logSize
			Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);
			for (Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
				TopicPartition partitionInfo = entry.getKey();
				// 获取offset
				String offsetPath = MessageFormat.format("/consumers/{0}/offsets/{1}/{2}", groupId,
						partitionInfo.topic(), partitionInfo.partition());
				byte[] data = zk.getData(offsetPath, false, null);
				long offset = Long.valueOf(new String(data));

				lagSum += endOffsets.get(partitionInfo) - offset;

				if (logger.isDebugEnabled()) {
					Map<String, Object> topicPatitionMap = new HashMap<>();
					topicPatitionMap.put("groupId", groupId);
					topicPatitionMap.put("topic", partitionInfo.topic());
					topicPatitionMap.put("partition", partitionInfo.partition());
					topicPatitionMap.put("logSize", endOffsets.get(partitionInfo));
					topicPatitionMap.put("offset", offset);
					topicPatitionMap.put("lag", endOffsets.get(partitionInfo) - offset);
					logger.debug("topicPatitionLagInfo: {}", topicPatitionMap);
					// topicPatitionMapList.add(topicPatitionMap);
				}
			}

			if (lagSum >= monitorConf.getWarnLimit()) {
				Map<String, Object> topicLagMap = new HashMap<>(2);
				topicLagMap.put("topic", topic);
				topicLagMap.put("lag", lagSum);
				overLimitTopicLagMapList.add(topicLagMap);
				logger.warn(MessageFormat.format("overWarnLimitInfo: [topic:{0}, groupId:{1}, lag: {2}]", topic,
						groupId, lagSum));
			}
		}

		// 发送告警短信
		if (overLimitTopicLagMapList.size() > 0) {
			sortTopicsLagListDesc(overLimitTopicLagMapList);
			
			StringBuilder msgContentBuilder = new StringBuilder("[");
			// 构造发送短信， 只显示部分topic 避免短信过长
			for (int i = 0; i < overLimitTopicLagMapList.size() && i < monitorConf.getMsgTopicsShowMax(); i++) {
				msgContentBuilder.append(overLimitTopicLagMapList.get(i).get("topic")).append(":")
						.append(overLimitTopicLagMapList.get(i).get("lag")).append(", ");
			}
			if (overLimitTopicLagMapList.size() > monitorConf.getMsgTopicsShowMax()) {
				msgContentBuilder.append("…]");
			} else {
				msgContentBuilder.setLength(msgContentBuilder.length() - 2);
				msgContentBuilder.append("]");
			}
			String content = MessageFormat.format(monitorConf.getMsgContent(), groupId, msgContentBuilder.toString(),
					monitorConf.getWarnLimit(), format.format(new Date()));
			logger.warn(MessageFormat.format("告警短信内容: content:{0}, target: {1}", content,
					monitorConf.getMsgTarget()));
			
			// 发送短信
			SmsSendUtil.sendMessage(monitorConf.getMsgBizCode(), monitorConf.getMsgBizName(),
					monitorConf.getMsgTaskName(), content, monitorConf.getMsgTarget());
		}

	}

	/**
	 * 按lag降序排列
	 * 
	 * @param lagOverLimitTopicsMapList
	 */
	private void sortTopicsLagListDesc(List<Map<String, Object>> lagOverLimitTopicsMapList) {
		Collections.sort(lagOverLimitTopicsMapList, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> map1, Map<String, Object> map2) {
				Long lag1 = (Long) map1.get("lag");
				Long lag2 = (Long) map2.get("lag");
				if (lag1 == null) {
					if (lag2 != null) {
						return 1;
					}
					return 0;
				} else {
					return 0 - lag1.compareTo(lag2);
				}
			}
		});
	}

}
