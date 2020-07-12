package com.itcast.canal_client.kafka;

import cn.itcast.canal.bean.RowData;
import com.itcast.canal_client.util.ConfigUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * kafka生产者工具类
 */
public class KafkaSender {
    //定义properties
    private Properties kafkaProps = new Properties();
    //这里的key依然是String，key的序列化是String序列化
    //value的序列化是我们自己定义的序列化方式，该序列化方式要求传递一个ProtoBufable对象或者其子类
    private KafkaProducer<String, RowData> kafkaProducer;

    public KafkaSender() {
        kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigUtil.kafkaBootstrap_servers_config());    //必写的
        kafkaProps.put(ProducerConfig.ACKS_CONFIG, ConfigUtil.kafkaAcks()); //可选， 1
        kafkaProps.put(ProducerConfig.RETRIES_CONFIG, ConfigUtil.kafkaRetries());   //可选的
        kafkaProps.put(ProducerConfig.BATCH_SIZE_CONFIG, ConfigUtil.kafkaBatch_size_config()); //可选的
        kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ConfigUtil.kafkaKey_serializer_class_config());//可选的
        kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ConfigUtil.kafkaValue_serializer_class_config());//可选的

        //实例化kafka生产者对象
        kafkaProducer = new KafkaProducer<String, RowData>(kafkaProps);
    }

    //发送数据到kafka中
    public void send(RowData rowData) {
        kafkaProducer.send(new ProducerRecord<>(ConfigUtil.kafkaTopic(), null, rowData));
    }
}
