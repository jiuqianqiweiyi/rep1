# rep1
我的rep1仓库
技术架构：Mysql+Canal+Kafka+Flink+Redis+Hbase+Phoenix+Druid
1、编写 canal 客户端程序,将 mysql 中增量数据写入 kafka；
2、编写全量维度数据的同步程序,将 mysql 中维度数据写入 redis 中;
3、编写增量维度数据的同步程序,数据导入过程:mysql → kafka → flink → redis; 
4、开发 FlinkCEP 与风控系统整合程序,筛选订单超时/其它异常数据,方便及时处理;
5、编写 redis 连接池/hbase 连接池等公共工具类开发。
