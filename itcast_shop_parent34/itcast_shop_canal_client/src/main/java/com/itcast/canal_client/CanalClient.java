package com.itcast.canal_client;

import cn.itcast.canal.bean.RowData;
import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.itcast.canal_client.kafka.KafkaSender;
import com.itcast.canal_client.util.ConfigUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * canal客户端
 * 与canal的服务端建立连接，然后获取canalserver的binlog日志
 */
public class CanalClient {
    // 一次性读取BINLOG数据条数
    private static final int BATCH_SIZE = 5 * 1024;
    // Canal客户端连接器
    private CanalConnector canalConnector;
    // Canal配置项
    private Properties properties;
    private KafkaSender kafkaSender;

    /**
     * 构造方法，初始化连接，初始化kafka对象
     */
    public CanalClient() {
        // 初始化连接
        canalConnector = CanalConnectors.newClusterConnector(ConfigUtil.zookeeperServerIp(),
                ConfigUtil.canalServerDestination(),
                ConfigUtil.canalServerUsername(),
                ConfigUtil.canalServerPassword());

        kafkaSender = new KafkaSender();
    }

    // 开始监听
    public void start() {
        try {
            while(true) {
                // 建立连接
                canalConnector.connect();
                // 回滚上次的get请求，重新获取数据
                canalConnector.rollback();
                // 订阅匹配日志
                canalConnector.subscribe(ConfigUtil.canalSubscribeFilter());
                while(true) {
                    // 批量拉取binlog日志，一次性获取多条数据
                    Message message = canalConnector.getWithoutAck(BATCH_SIZE);
                    // 获取batchId
                    long batchId = message.getId();
                    // 获取binlog数据的条数
                    int size = message.getEntries().size();
                    if(batchId == -1 || size == 0) {

                    }
                    else {
                        Map binlogMsgMap = binlogMessageToMap(message);
                        RowData rowData = new RowData(binlogMsgMap);
                        System.out.println(rowData.toString());
                        if (binlogMsgMap.size() > 0) {
                            kafkaSender.send(rowData);
                        }
                    }
                    // 确认指定的batchId已经消费成功
                    canalConnector.ack(batchId);
                }
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } finally {
            // 断开连接
            canalConnector.disconnect();
        }
    }

    /**
     * 将binlog日志转换为Map结构
     * @param message
     * @return
     */
    private Map binlogMessageToMap(Message message) throws InvalidProtocolBufferException {
        Map rowDataMap = new HashMap();

        // 1. 遍历message中的所有binlog实体
        for (CanalEntry.Entry entry : message.getEntries()) {
            // 只处理事务型binlog
            if(entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN ||
                    entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }

            // 获取binlog文件名
            String logfileName = entry.getHeader().getLogfileName();
            // 获取logfile的偏移量
            long logfileOffset = entry.getHeader().getLogfileOffset();
            // 获取sql语句执行时间戳
            long executeTime = entry.getHeader().getExecuteTime();
            // 获取数据库名
            String schemaName = entry.getHeader().getSchemaName();
            // 获取表名
            String tableName = entry.getHeader().getTableName();
            // 获取事件类型 insert/update/delete
            String eventType = entry.getHeader().getEventType().toString().toLowerCase();

            rowDataMap.put("logfileName", logfileName);
            rowDataMap.put("logfileOffset", logfileOffset);
            rowDataMap.put("executeTime", executeTime);
            rowDataMap.put("schemaName", schemaName);
            rowDataMap.put("tableName", tableName);
            rowDataMap.put("eventType", eventType);

            // 获取所有行上的变更
            Map<String, String> columnDataMap = new HashMap<>();
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            List<CanalEntry.RowData> columnDataList = rowChange.getRowDatasList();
            for (CanalEntry.RowData rowData : columnDataList) {
                if(eventType.equals("insert") || eventType.equals("update")) {
                    for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                        columnDataMap.put(column.getName(), column.getValue().toString());
                    }
                }
                else if(eventType.equals("delete")) {
                    for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
                        columnDataMap.put(column.getName(), column.getValue().toString());
                    }
                }
            }

            rowDataMap.put("columns", columnDataMap);
        }

        return rowDataMap;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
