package cn.itheima.cep;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.IngestionTimeExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.List;
import java.util.Map;


/**
 * 系统监控：
 * 需求：
 * - 输入事件流由来自一组机架的温度和功率事件组成
 * - 目标是检测当机架过热时我们需要发出警告和报警
 *   - 警告：某机架在10秒内连续两次上报的温度超过阈值
 *   - 报警：某机架在20秒内连续两次匹配警告，并且第二次的温度超过了第一次的温度就告警
 *
 *  需求分析：
 *      警告的前提是10秒内连续两次上报温度超过阈值
 *      报警的前提是20秒内连续两次警告，并且第二次的警告超过了第一次警告的温度，两者之间是有依赖关系，报警所操作的数据集是警告的结果集
 */
public class SystemMoitoring {
    //定义温度的阈值
    private static  final double TEMPPERATURE_THEWHOLD  = 100;

    /**
     * 入口方法
     * @param args
     */
    public static void main(String[] args) throws Exception {
        //1：初始化运行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        //2：设置按照事件来处理数据
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        //不指定并行度，那么现在有几个线程去执行？8个线程去执行

        //3：接入数据源（正常的话这些数据来自于kafka集群，我们这里写了一个模拟器不停生成数据）
        //要求按照事件时间处理数据，但是事件中没有时间，所以在这里定义了一个抽取数据的时间作为事件时间
        SingleOutputStreamOperator<MonitoringEvent> singleOutputStreamOperator = env.addSource(new MonitoringEventSource())
                .assignTimestampsAndWatermarks(new IngestionTimeExtractor<>());

        //singleOutputStreamOperator.print("上报的数据>>>");

        //4：实现业务处理
        /***
         * 警告：某机架在10秒内连续两次上报的温度超过阈值
         * 4.1：定义模式规则
         * 4.2：将模式规则应用到数据流中
         * 4.3：获取匹配到的警告数据
         */
        //4.1：定义模式规则
        Pattern<MonitoringEvent, TemperatureEvent> waringPattern = Pattern.<MonitoringEvent>begin("first").subtype(TemperatureEvent.class).where(new IterativeCondition<TemperatureEvent>() {
            @Override
            public boolean filter(TemperatureEvent temperatureEvent, Context<TemperatureEvent> context) throws Exception {
                return temperatureEvent.getTemperature() > TEMPPERATURE_THEWHOLD;
            }
        }).next("second").subtype(TemperatureEvent.class).where(new IterativeCondition<TemperatureEvent>() {
            @Override
            public boolean filter(TemperatureEvent temperatureEvent, Context<TemperatureEvent> context) throws Exception {
                return temperatureEvent.getTemperature() > TEMPPERATURE_THEWHOLD;
            }
        }).within(Time.seconds(10));

        //4.2：将模式规则应用到数据流中
        PatternStream<MonitoringEvent> waringPatternStream = CEP.pattern(singleOutputStreamOperator.keyBy("rackID"), waringPattern);

        //4.3：获取匹配到的警告数据
        SingleOutputStreamOperator<TemperatureWarning> waring = waringPatternStream.select(new PatternSelectFunction<MonitoringEvent, TemperatureWarning>() {
            @Override
            public TemperatureWarning select(Map<String, List<MonitoringEvent>> pattern) throws Exception {
                //找到匹配上的两次温度数据
                TemperatureEvent first = (TemperatureEvent) pattern.get("first").get(0);
                TemperatureEvent second = (TemperatureEvent) pattern.get("second").get(0);

                //返回警告流的数据对象
                return new TemperatureWarning(first.getRackID(), (first.getTemperature() + second.getTemperature()) / 2);
            }
        });

        //打印警告流数据
        waring.print("警告流数据>>>");

        /**
         * 5：业务处理
         * 报警：某机架在20秒内连续两次匹配警告，并且第二次的温度超过了第一次的温度就告警
         * 分析：
         * 第二次的温度要超过第一次的温度才能够满足报警的要求，怎么做？因为在规则模型定义的时候判断的条件是写的固定值，而不是一个可以变化的值
         */
        //5.1：定义报警规则
        Pattern<TemperatureWarning, TemperatureWarning> alterPattern = Pattern.<TemperatureWarning>begin("first").next("second").within(Time.seconds(20));

        //5.2：将报警的规则应用到数据流中
        PatternStream<TemperatureWarning> alterPatternStream = CEP.pattern(waring.keyBy("rackID"), alterPattern);

        //5.3：获取到报警的数据流
        SingleOutputStreamOperator<TemperatureAlert> alter = alterPatternStream.select(new PatternSelectFunction<TemperatureWarning, TemperatureAlert>() {
            @Override
            public TemperatureAlert select(Map<String, List<TemperatureWarning>> pattern) throws Exception {
                //找到警告流上的两次温度事件
                TemperatureWarning first = (TemperatureWarning) pattern.get("first").get(0);
                TemperatureWarning second = (TemperatureWarning) pattern.get("second").get(0);

                //判断第二次的温度是否超过了第一次的温度
                if (second.getAverageTemperature() > first.getAverageTemperature()) {
                    //返回报警实体对象
                    return new TemperatureAlert(first.getRackID());
                } else {
                    return null;
                }
            }
        });

        //打印报警流数据
        alter.filter(new FilterFunction<TemperatureAlert>() {
            @Override
            public boolean filter(TemperatureAlert value) throws Exception {
                return value != null;
            }
        }).printToErr("报警流数据>>>");

        //启动任务
        env.execute();
    }
}
