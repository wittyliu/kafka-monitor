package cn.wittyliu.kafka.monitor.util;

import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;

public class KafkaUtil {

	public static Properties getConsumerProperties(String groupId, String bootstrap_servers) {
		Properties props = new Properties();
		props.put("group.id", groupId);
		props.put("bootstrap.servers", bootstrap_servers);
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		return props;
	}
	
	
	public static KafkaConsumer<String, String> getKafkaConsumer(Properties props){
		return new KafkaConsumer<>(props);
	}
	public static KafkaConsumer<String, String> getKafkaConsumer(String groupId, String bootstrap_servers){
		return new KafkaConsumer<>(getConsumerProperties(groupId, bootstrap_servers));
	}
}
