package cn.itcast.cep

import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * 需求：
 * 检测哪些用户是15分钟完成支付的用户，哪些是15分钟内没有支付的用户
 * 15分钟内完成下单支付的用户过滤出来
 * 超过15分钟支付的用户（默认丢弃了）但是我们也希望将15分钟后完成支付的用户保存起来
 */
object OrderTimeoutDemo {
  //定义订单的实体对象
  case class OrderEvent(orderID:Long, orderStatus:String, timestamp:Long)
  //定义匹配到的数据的实体对象
  //payStatus：取值范围（正常支付、超时支付）
  case class OrderResult(orderID:Long, payStatus:String)

  def main(args: Array[String]): Unit = {
    //1：初始化运行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //2：设置并行度为1
    env.setParallelism(1)

    //3：设置按照事件事件来处理数据
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    //4：构建数据源
    val orderEventDataStream: DataStream[OrderEvent] = env.fromCollection(List(
        OrderEvent(1, "create", 1558430842), //2019-05-21 17:27:22
        OrderEvent(2, "create", 1558430843), //2019-05-21 17:27:23
        OrderEvent(2, "other", 1558430845), //2019-05-21 17:27:25
        OrderEvent(2, "pay", 1558430850), //2019-05-21 17:27:30
        OrderEvent(1, "pay", 1558431920) //2019-05-21 17:45:20
      )).assignAscendingTimestamps(_.timestamp *1000)

    //5：定义模式规则
    val orderPayPattern: Pattern[OrderEvent, OrderEvent] = Pattern.begin[OrderEvent]("begin")
      .where(_.orderStatus == "create")
      .followedBy("second")
      .where(_.orderStatus == "pay")
      .within(Time.minutes(15)) //15分钟内完成下单支付的用户过滤出来

    //6：将模式规则应用到数据流中
    val patternDataStream: PatternStream[OrderEvent] = CEP.pattern(orderEventDataStream.keyBy(_.orderID), orderPayPattern)

    //定义一个输出标签，这个标签用来标记超时的订单数据(指定的数据类型就是要返回的数据类型)
    val orderTimeoutTag: OutputTag[OrderResult] = OutputTag[OrderResult]("orderTimeout")

    //7：获取到符合条件的数据（符合15分钟完成下单支付的用户），不符合条件的用户（15分钟未完成下单支付操作的用户）也要取到
    import scala.collection.Map
    val resultDataStream: DataStream[OrderResult] = patternDataStream.select(orderTimeoutTag)((pattern: Map[String, Iterable[OrderEvent]], timestamp: Long) => {
      //用于处理订单超时的数据
      val payOrderId: Long = pattern.getOrElse("begin", null).iterator.next().orderID
      //返回超时支付的订单
      OrderResult(payOrderId, "超时订单")
    })((pattern: Map[String, Iterable[OrderEvent]]) => {
      //处理的是符合条件的数据
      val payOrderId: Long = pattern.getOrElse("begin", null).iterator.next().orderID
      //返回正常支付的订单
      OrderResult(payOrderId, "正常支付")
    })

    //8：打印未超时的订单
    resultDataStream.print("正常完成支付的订单>>>")

    //9：打印超时支付的订单
    val timeoutDataStream: DataStream[OrderResult] = resultDataStream.getSideOutput(orderTimeoutTag)
    timeoutDataStream.printToErr("超时完成支付的订单>>>")

    //10：启动任务
    env.execute()
  }
}
