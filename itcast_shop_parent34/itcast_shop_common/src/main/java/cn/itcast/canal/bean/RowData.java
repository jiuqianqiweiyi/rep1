package cn.itcast.canal.bean;

import cn.itcast.canal.protobuf.CanalModel;
import cn.itcast.canal.protobuf.ProtoBufable;
import com.alibaba.fastjson.JSON;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.HashMap;
import java.util.Map;

/**
 * 能够使用protoBuf序列化的bean对象，要求必须继承ProtoBufable接口
 */
public class RowData implements ProtoBufable {

    public String getLogfileName() {
        return logfileName;
    }

    public void setLogfileName(String logfileName) {
        this.logfileName = logfileName;
    }

    public Long getLogfileOffset() {
        return logfileOffset;
    }

    public void setLogfileOffset(Long logfileOffset) {
        this.logfileOffset = logfileOffset;
    }

    public Long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, String> columns) {
        this.columns = columns;
    }

    private String logfileName;
    private Long logfileOffset;
    private Long executeTime;
    private String schemaName;
    private String tableName;
    private String eventType;
    private Map<String, String> columns;

    /**
     * 构造方法，传递进来的是封装好的binlog日志，这个日志是canal客户端直接在server端获取到的数据
     * @param map
     */
    public RowData(Map map){
        //说明有数据
        if(map.size() > 0){
            this.logfileName = map.get("logfileName").toString();
            this.logfileOffset = Long.parseLong(map.get("logfileOffset").toString());
            this.executeTime = Long.parseLong(map.get("executeTime").toString());
            this.schemaName = map.get("schemaName").toString();
            this.tableName = map.get("tableName").toString();
            this.eventType = map.get("eventType").toString();
            this.columns = (Map<String, String>)map.get("columns");
        }
    }

    /**
     * 传递读取到的kafka中的RowData对象字节码数据，将读取到的字节码转换成RowData对象
     * 相当于反序列化的操作
     * @param bytes
     */
    public RowData(byte[] bytes){
        try{
            //解析字节码数据
            CanalModel.RowData rowData = CanalModel.RowData.parseFrom(bytes);
            this.logfileName = rowData.getLogfileName();
            this.logfileOffset =rowData.getLogfileOffset();
            this.executeTime = rowData.getExecuteTime();
            this.tableName = rowData.getTableName();
            this.eventType = rowData.getEventType();
            this.schemaName = rowData.getSchemaName();
            //将所有的列的集合添加到Map中
            this.columns = new HashMap<>();
            columns.putAll(rowData.getColumnsMap());
        }catch (Exception ex){
            throw  new RuntimeException(ex);
        }
    }

    /**
     * 这是核心方法，这个方法是需要接受到的binlog日志解析后的属性赋值给protoBuf的bean，返回返回
     * 序列化后的protobuf的字节码
     * @return
     */
    @Override
    public byte[] toByte() {
        CanalModel.RowData.Builder builder = CanalModel.RowData.newBuilder();
        builder.setLogfileName(this.logfileName);
        builder.setLogfileOffset(this.logfileOffset);
        builder.setExecuteTime(this.executeTime);
        builder.setSchemaName(this.schemaName);
        builder.setTableName(this.tableName);
        builder.setEventType(this.eventType);
        for(String key:this.columns.keySet()){
            builder.putColumns(key, this.columns.get(key));
        }
        return builder.build().toByteArray();
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
