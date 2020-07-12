package cn.itcast.cep

import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * 用户在10s内，同时输入TMD超过五次，认为这个用户是恶意用户，识别出来这个用户
 * 使用FlinkCEP的方式检测刷屏用户
 */
object CheckUserEventDemo {

  //定义事件的样例类
  case class Message(userId:String, msg:String, timestamp:Long)

  def main(args: Array[String]): Unit = {
    //1：初始化运行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //2：设置并行度
    env.setParallelism(1)

    //3：指定数据处理方式按照事件时间进行处理
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    //4：构建数据源
    val loginEventStream: DataStream[Message] = env.fromCollection(
      List(
        Message("1", "TMD", 1558430842),    //2019-05-21 17:27:22
        Message("1", "TMD", 1558430843),    //2019-05-21 17:27:23
        Message("1", "TMD", 1558430845),    //2019-05-21 17:27:25
        Message("1", "TMD", 1558430850),    //2019-05-21 17:27:30
        Message("1", "TMD", 1558430851),    //2019-05-21 17:27:31
        Message("1", "TMD", 1558430852)     //2019-05-21 17:27:32
      )
    ).assignAscendingTimestamps(_.timestamp * 1000)

    //5：定义模式规则
    val messagePattern: Pattern[Message, Message] = Pattern.begin[Message]("begin")
      .where(_.msg == "TMD").times(5)  //TNND\TMD\....
      .within(Time.seconds(10)) //10秒内输入五次TMD，判定为恶意用户

    //6：将模式规则应用到数据源中
    val patternStream: PatternStream[Message] = CEP.pattern(loginEventStream.keyBy(_.userId), messagePattern)

    //7：获取匹配到的数据
    //这里用到的map对象，key是string类型，value是一个迭代器
    //key：是状态的名字
    //value：状态中存放的数据集合
    import scala.collection.Map
    val messageDataStream: DataStream[(String, Long)] = patternStream.select((patten: Map[String, Iterable[Message]]) => {
      val begin: Message = patten.getOrElse("begin", null).iterator.next()

      //将查询到的数据返回
      (begin.userId, begin.timestamp)
    })

    //8：打印匹配到的用户数据
    messageDataStream.printToErr("匹配到的恶意用户>>>")

    //9：执行任务
    env.execute()
  }
}
