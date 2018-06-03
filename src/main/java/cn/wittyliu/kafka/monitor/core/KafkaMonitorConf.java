package cn.wittyliu.kafka.monitor.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import cn.wittyliu.kafka.monitor.handler.WarnExceptionHandlerType;
import cn.wittyliu.kafka.monitor.util.StringUtil;


public class KafkaMonitorConf {

	public static final String PROPERTY_NAME = "kafka-monitor-config.properties";
	public static final String EMPTY_EXCEPTION_PREFIX = "配置";

	
	private String zkConnString;
	private int zkTimeout = 30000;
	private String bootstrapServers;
	private String groupId;
	private String topicPattStr;
	private List<Pattern> topics = new ArrayList<>();
	private int warnLimit = 20000;
	private int warnInterval = 600; // 秒
	private int warnTimeout = 60 * 30; // 告警监控任务超时时间 秒
	private int warnExceptionTimes = 10; // 连续失败次数限制
	private volatile AtomicInteger leftWarnExceptionTimes;
	private WarnExceptionHandlerType warnExceptionHandlerType = WarnExceptionHandlerType.WARN; // 连续失败处理

	private int msgTopicsShowMax = 4; // 告警短信最多显示的topic数 避免短信过长
	private String msgBizCode;
	private String msgBizName;
	private String msgTaskName;
	private String msgContent;
	private List<String> msgTarget = new ArrayList<>();

	public void loadConfig(String filePath, boolean isArg) throws IOException {

		File file = new File(filePath).getCanonicalFile();
		InputStream input = null;
		try {
			if (file.exists()) {
				System.out.println("加载配置文件" + file.getAbsolutePath());
				input = new FileInputStream(file);
			} else if (isArg) {
				throw new FileNotFoundException("文件" + file.getAbsolutePath() + "不存在!");
			} else {
				System.out.println("当前目录没有找到配置文件" + PROPERTY_NAME + "\n启用jar包内配置文件");
				input = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTY_NAME);
				if (input == null) {
					throw new IOException("jar包内没有找到配置文件" + PROPERTY_NAME);
				}
			}

			Properties prop = new Properties();
			prop.load(input);
			this.zkConnString = StringUtil.getTrimString(prop, "zookeeper.connString", false, EMPTY_EXCEPTION_PREFIX);
			this.zkTimeout = StringUtil.getInt(prop, "zookeeper.timeout", this.zkTimeout);

			this.bootstrapServers = StringUtil.getTrimString(prop, "kafka.bootstrap_servers", false,
					EMPTY_EXCEPTION_PREFIX);
			this.groupId = StringUtil.getTrimString(prop, "kafka.groupId", false, EMPTY_EXCEPTION_PREFIX);
			this.topicPattStr = StringUtil.getTrimString(prop, "kafka.topics.pattern", true, EMPTY_EXCEPTION_PREFIX);
			if (this.topicPattStr != null) {
				String[] topicStrArr = this.topicPattStr.split(",");
				for (int i = 0; i < topicStrArr.length; i++) {
					String topicPatt = topicStrArr[i].trim();
					this.topics.add(Pattern.compile(topicPatt));
				}
			}

			this.warnLimit = StringUtil.getInt(prop, "monitor.warn.limit", this.warnLimit);
			this.warnInterval = StringUtil.getInt(prop, "monitor.warn.task.interval", this.warnInterval);
			this.warnTimeout = StringUtil.getInt(prop, "monitor.warn.task.timeout", this.warnTimeout);
			this.warnExceptionTimes = StringUtil.getInt(prop, "monitor.warn.task.exception.times", this.warnExceptionTimes);
			this.leftWarnExceptionTimes = new AtomicInteger(this.warnExceptionTimes);
			String warnExceptionHandlerType = StringUtil.getTrimString(prop, "monitor.warn.task.exception.handler.type",
					true);
			if (warnExceptionHandlerType != null) {
				this.warnExceptionHandlerType = WarnExceptionHandlerType.valueOf(warnExceptionHandlerType);
			}

			this.msgTopicsShowMax = StringUtil.getInt(prop, "message.topics.show.max", this.msgTopicsShowMax);
			this.msgBizCode = StringUtil.getTrimString(prop, "message.bizCode", false, EMPTY_EXCEPTION_PREFIX);
			this.msgBizName = StringUtil.getTrimString(prop, "message.bizName", false, EMPTY_EXCEPTION_PREFIX);
			this.msgTaskName = StringUtil.getTrimString(prop, "message.taskName", false, EMPTY_EXCEPTION_PREFIX);
			this.msgContent = StringUtil.getTrimString(prop, "message.content", false, EMPTY_EXCEPTION_PREFIX);
			String msgTargetString = StringUtil.getTrimString(prop, "message.target", false, EMPTY_EXCEPTION_PREFIX);

			String[] msgTargetArr = msgTargetString.split(",");
			for (int i = 0; i < msgTargetArr.length; i++) {
				String target = msgTargetArr[i].trim();
				if (!this.msgTarget.contains(target)) {
					this.msgTarget.add(target);
				}
			}

			// 乱码
			this.msgBizName = new String(this.msgBizName.getBytes(Charset.forName("ISO-8859-1")),
					Charset.forName("GBK"));
			this.msgTaskName = new String(this.msgTaskName.getBytes(Charset.forName("ISO-8859-1")),
					Charset.forName("GBK"));
			this.msgContent = new String(this.msgContent.getBytes(Charset.forName("ISO-8859-1")),
					Charset.forName("GBK"));

		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String getZkConnString() {
		return zkConnString;
	}

	public void setZkConnString(String zkConnString) {
		this.zkConnString = zkConnString;
	}

	public int getZkTimeout() {
		return zkTimeout;
	}

	public void setZkTimeout(int zkTimeout) {
		this.zkTimeout = zkTimeout;
	}

	public String getBootstrapServers() {
		return bootstrapServers;
	}

	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getTopicPattStr() {
		return topicPattStr;
	}

	public void setTopicPattStr(String topicPattStr) {
		this.topicPattStr = topicPattStr;
	}

	public List<Pattern> getTopics() {
		return topics;
	}

	public void setTopics(List<Pattern> topics) {
		this.topics = topics;
	}

	public int getWarnLimit() {
		return warnLimit;
	}

	public void setWarnLimit(int warnLimit) {
		this.warnLimit = warnLimit;
	}

	public int getWarnInterval() {
		return warnInterval;
	}

	public void setWarnInterval(int warnInterval) {
		this.warnInterval = warnInterval;
	}

	public int getWarnTimeout() {
		return warnTimeout;
	}

	public void setWarnTimeout(int warnTimeout) {
		this.warnTimeout = warnTimeout;
	}

	public int getWarnExceptionTimes() {
		return warnExceptionTimes;
	}

	public void setWarnExceptionTimes(int warnExceptionTimes) {
		this.warnExceptionTimes = warnExceptionTimes;
	}

	public AtomicInteger getLeftWarnExceptionTimes() {
		return leftWarnExceptionTimes;
	}

	public void setLeftWarnExceptionTimes(AtomicInteger leftWarnExceptionTimes) {
		this.leftWarnExceptionTimes = leftWarnExceptionTimes;
	}

	public WarnExceptionHandlerType getWarnExceptionHandlerType() {
		return warnExceptionHandlerType;
	}

	public void setWarnExceptionHandlerType(WarnExceptionHandlerType warnExceptionHandlerType) {
		this.warnExceptionHandlerType = warnExceptionHandlerType;
	}

	public int getMsgTopicsShowMax() {
		return msgTopicsShowMax;
	}

	public void setMsgTopicsShowMax(int msgTopicsShowMax) {
		this.msgTopicsShowMax = msgTopicsShowMax;
	}

	public String getMsgBizCode() {
		return msgBizCode;
	}

	public void setMsgBizCode(String msgBizCode) {
		this.msgBizCode = msgBizCode;
	}

	public String getMsgBizName() {
		return msgBizName;
	}

	public void setMsgBizName(String msgBizName) {
		this.msgBizName = msgBizName;
	}

	public String getMsgTaskName() {
		return msgTaskName;
	}

	public void setMsgTaskName(String msgTaskName) {
		this.msgTaskName = msgTaskName;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}

	public List<String> getMsgTarget() {
		return msgTarget;
	}

	public void setMsgTarget(List<String> msgTarget) {
		this.msgTarget = msgTarget;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("KafkaMonitorConf [zkConnString=").append(zkConnString).append(", zkTimeout=").append(zkTimeout)
				.append(", bootstrapServers=").append(bootstrapServers).append(", groupId=").append(groupId)
				.append(", topicPattStr=").append(topicPattStr).append(", topics=").append(topics)
				.append(", warnLimit=").append(warnLimit).append(", warnInterval=").append(warnInterval)
				.append(", warnTimeout=").append(warnTimeout).append(", warnExceptionTimes=").append(warnExceptionTimes)
				.append(", warnExceptionHandlerType=").append(warnExceptionHandlerType).append(", msgTopicsShowMax=")
				.append(msgTopicsShowMax).append(", msgBizCode=").append(msgBizCode).append(", msgBizName=")
				.append(msgBizName).append(", msgTaskName=").append(msgTaskName).append(", msgContent=")
				.append(msgContent).append(", msgTarget=").append(msgTarget).append("]");
		return builder.toString();
	}

}
