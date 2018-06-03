package cn.wittyliu.kafka.monitor.handler;

public enum WarnExceptionHandlerType {
	NOTHING("不处理"), WARN("告警"), EXIT("退出"), WARN_TRY_EXIT("告警并尝试退出");
	
	private String caption;
	WarnExceptionHandlerType(String caption) {
		this.caption = caption;
	}
	public String getCaption() {
		return caption;
	}
	
}
