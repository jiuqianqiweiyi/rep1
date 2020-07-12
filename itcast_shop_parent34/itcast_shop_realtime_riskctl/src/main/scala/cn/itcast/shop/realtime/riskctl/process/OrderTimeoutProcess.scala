package cn.itcast.shop.realtime.riskctl.process

import cn.itcast.canal.bean.RowData
import cn.itcast.shop.realtime.riskctl.bean.OrderDBEntity
import cn.itcast.shop.realtime.riskctl.utils.GlobalConfigUtil
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * 订单超时处理的实现类
 * 将十五分钟内没有完成支付的用户统计出来写入到Druid中供OLAP分析
 * @param env
 */
class OrderTimeoutProcess(env:StreamExecutionEnvironment) extends MysqlBaseCEP(env) {
  /**
   * 处理数据的接口
   */
  override def process(): Unit = {
    //1：过滤出来订单数据
    val orderCanalDataStream: DataStream[RowData] = getKafkaDataStream().filter(_.getTableName=="itcast_orders").filter(_.getColumns.size()>0)

    //2：讲订单数据转换成订单对象
    val orderDBEntityDataStream: DataStream[OrderDBEntity] = orderCanalDataStream.map(canalEntity=>{
      OrderDBEntity(canalEntity)
    })

    //3：定义模式规则
    val orderPayPattern: Pattern[OrderDBEntity, OrderDBEntity] = Pattern.begin[OrderDBEntity]("first")
      //表示这个订单没有支付并且待发货状态（create）
      .where(_.isPay == 0).where(_.orderStatus == 0)
      .followedBy("second")
      //表示这个订单已经支付并且待发货状态（payed， 状态每次修改都要匹配）
      .where(_.isPay == 1).where(_.orderStatus == 0)
      .within(Time.minutes(15))

    //4: 将规则应用到数据流中
    val patternDataStream: PatternStream[OrderDBEntity] = CEP.pattern(orderDBEntityDataStream.keyBy(_.orderId), orderPayPattern)

    //5：获取匹配到的规则数据
    //定义一个输出标签，这个标签用来标记超时的订单数据(指定的数据类型就是要返回的数据类型)
    val orderTimeoutTag: OutputTag[OrderDBEntity] = OutputTag[OrderDBEntity]("orderTimeout")

    //7：获取到符合条件的数据（符合15分钟完成下单支付的用户），不符合条件的用户（15分钟未完成下单支付操作的用户）也要取到
    import scala.collection.Map
    val resultDataStream: DataStream[OrderDBEntity] = patternDataStream.select(orderTimeoutTag)((pattern: Map[String, Iterable[OrderDBEntity]], timestamp: Long) => {
      //用于处理订单超时的数据
      val payOrder = pattern.getOrElse("begin", null).iterator.next()
      //返回超时支付的订单
      payOrder
    })((pattern: Map[String, Iterable[OrderDBEntity]]) => {
      //处理的是符合条件的数据
      val payOrder = pattern.getOrElse("begin", null).iterator.next()
      //返回正常支付的订单
      payOrder
    })

    //打印未超时的数据
    resultDataStream.print("正常完成支付的订单>>>")

    //将超时支付的数据写入到kafka中
    val timeoutDataStream: DataStream[OrderDBEntity] = resultDataStream.getSideOutput(orderTimeoutTag)
    val timeoutJsonDataStream: DataStream[String] = timeoutDataStream.map(orderEntity => {
      JSON.toJSONString(orderEntity, SerializerFeature.DisableCircularReferenceDetect)
    })

    //将超时支付的订单写入到kafka集群
    timeoutJsonDataStream.addSink(kafkaProducer(GlobalConfigUtil.`output.topic.ordertimeout`))
  }
}
