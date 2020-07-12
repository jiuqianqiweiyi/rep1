package cn.itcast.shop.realtime.riskctl.app

import cn.itcast.shop.realtime.riskctl.process.OrderTimeoutProcess
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

/**
 * 风控系统
 */
object App {
  def main(args: Array[String]): Unit = {
    /**
     * 实现思路：
     * 1：初始化flink的运行环境
     * 2：设置flink的开发环境的并行度为1，生产环境不可以设置为1，可以不设置
     * 3：开启flink的checkpoint
     * 4：接入kafka的数据源，消费kafka中的数据
     * 5：实现所有的ETL业务
     * 6：执行任务
     */
    //TODO 1：初始化flink的运行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //TODO 2：设置flink的开发环境的并行度为1，生产环境不可以设置为1，可以不设置
    env.setParallelism(1)

    //TODO 3：开启flink的checkpoint
    //开启checkpoint的时候，设置checkpoint的执行周期，每5秒钟做一次checkpoint
    env.enableCheckpointing(5000)
    //如果程序被cancel，保留以前做的checkpoint，避免数据丢失
    env.getCheckpointConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    //同一个时间只有一个检查点，检查点的操作是否可以并行，1不并行
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
    //指定重启策略，默认的重启策略是不停的重启
    //程序出现异常的时候会重启，重启五次，每次延迟5秒钟，如果超过了5次，程序退出
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(5, 5000))

    //1：超时订单实时处理
   // val orderTimeoutProcess = new OrderTimeoutProcess(env)
   // orderTimeoutProcess.process()


    //执行任务
    //env.execute()
  }
}
