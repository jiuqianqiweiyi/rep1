package cn.itheima.cep;

/**
 * 首先我们定义一个监控事件
 */
public abstract class MonitoringEvent {
    private int rackID;

    public MonitoringEvent(int rackID) {
        this.rackID = rackID;
    }

    //获取机架id
    public int getRackID() {
        return rackID;
    }

    //设置机架id
    public void setRackID(int rackID) {
        this.rackID = rackID;
    }
}