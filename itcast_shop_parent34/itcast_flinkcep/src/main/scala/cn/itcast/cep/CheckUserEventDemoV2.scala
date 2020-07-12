package cn.itcast.cep

import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * 过滤出来用户输入同一句话超过五次，10秒内
 * 使用Flink CEP
 * 思路：
 * 可以将每五条数据划分成一个组，比如说有六条数据，会被划分成2个组，然后对组内的数据进行去重（msg），如果去重以后=1数据，说明组内的msg都是一样的
 */
object CheckUserEventDemoV2 {
  //定义事件的样例类
  case class Message(userId: String, ip: String, msg: String)

  def main(args: Array[String]): Unit = {
    //1：初始化运行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //2：设置并行度
    env.setParallelism(1)

    //4：构建数据源
    val loginEventStream: DataStream[Message] = env.fromCollection(
      List(
        Message("1", "192.168.0.1", "beijing"),
        Message("1", "192.168.0.2", "beijing"),
        Message("1", "192.168.0.3", "beijing"),
        Message("1", "192.168.0.4", "beijing"),
        Message("2", "192.168.10.10", "shanghai"),
        Message("3", "192.168.10.10", "beijing"),
        Message("3", "192.168.10.11", "beijing"),
        Message("4", "192.168.10.10", "beijing"),
        Message("5", "192.168.10.11", "shanghai"),
        Message("4", "192.168.10.12", "beijing"),
        Message("5", "192.168.10.13", "shanghai"),
        Message("5", "192.168.10.14", "shanghai"),
        Message("5", "192.168.10.15", "beijing"),
        Message("6", "192.168.10.16", "beijing"),
        Message("6", "192.168.10.17", "beijing"),
        Message("6", "192.168.10.18", "beijing"),
        Message("5", "192.168.10.18", "shanghai"),
        Message("6", "192.168.10.19", "beijing"),
        Message("6", "192.168.10.19", "beijing"),
        Message("5", "192.168.10.18", "shanghai"),
        Message("6", "192.168.10.19", "shanghai")
      )
    )

    //5：定义模式规则
    val messagePattern: Pattern[Message, Message] = Pattern.begin[Message]("begin")
      .where(_.msg != null).times(5) //不是空的数据是不是同一句话？
      .within(Time.seconds(10)) //10秒内输入五次TMD，判定为恶意用户

    //6：将模式规则应用到数据源中
    val patternStream: PatternStream[Message] = CEP.pattern(loginEventStream.keyBy(_.userId), messagePattern)

    //7：获取匹配到的数据
    //这里用到的map对象，key是string类型，value是一个迭代器
    //key：是状态的名字
    //value：状态中存放的数据集合
    import scala.collection.Map
    val messageResult: DataStream[Option[Iterable[Message]]] = patternStream.select((pattern: Map[String, Iterable[Message]]) => {
      //找到匹配规则的数据（查找符合条件的数据）
      val maybeMessages: Option[Iterable[Message]] = pattern.get("begin")
      //使用模式匹配将数据进行查找符合条件的
      maybeMessages match {
        case Some(value) => {
          //有数据
          if (value.map(_.msg).toList.distinct.size == 1) {
            Some(value)
          } else {
            None
          }
        }
      }
    })

    //8：打印匹配到的用户数据
    messageResult.filter(_!=None).map(x=> {
      x match {
        case Some(value) => value
      }
    }).printToErr("恶意用户>>>")

    messageResult.filter(_!=None).printToErr("恶意用户>>>")

    //9：执行任务
    env.execute()
  }
}

