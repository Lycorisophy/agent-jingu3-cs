package cn.lysoy.jingu3.cron;

/**
 * 定时任务作用域：全局或绑定单会话。
 */
public enum ScheduledTaskScope {
    GLOBAL,
    CONVERSATION
}
