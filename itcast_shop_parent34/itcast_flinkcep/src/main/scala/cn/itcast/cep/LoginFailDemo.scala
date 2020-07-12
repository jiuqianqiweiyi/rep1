package cn.itcast.cep

import java.util

import org.apache.flink.cep.PatternSelectFunction
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * 使用CEP来实现两秒内登录失败两次的用户
 */
object LoginFailDemo {

  //构建登录对象的实体类
  case class LoginEvent(userId:Long, ip:String, eventType:String, eventTime:Long)

  def main(args: Array[String]): Unit = {
    //1：初始化运行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //2：设置并行度为1
    env.setParallelism(1)

    //3：指定数据按照事件时间进行处理
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    //4：构建数据源
    val loginEventStream: DataStream[LoginEvent] = env.fromCollection(List(
        LoginEvent(1, "192.168.0.1", "fail", 1558430842), //2019-05-21 17:27:22
        LoginEvent(1, "192.168.0.2", "fail", 1558430843), //2019-05-21 17:27:23
        LoginEvent(1, "192.168.0.3", "success", 1558430844), //2019-05-21 17:27:24
        LoginEvent(2, "192.168.10.10", "success", 1558430845) //2019-05-21 17:27:25
    )).assignAscendingTimestamps(_.eventTime * 1000)

    /**
     * CEP是由数据和规则组成
     * 数据就是流式数据，源源不断产生的数据，规则就是固定的静态的，是一组预定的规则模型
     * 5：处理数据
     * 5.1：定义规则模型
     * 5.2：将规则模型应用到数据流中，进行匹配数据
     * 5.3：拿到获取到的符合规则的数据
     *
     * 在flinkcep中规则模型的定义，分为三种状态：起始状态、中间状态、结束状态，每一个状态都有一个条件，也可以是无条件的，
     * 可以将符合这个条件的数据放到这个状态中（state）
     * begin：表示起始状态
     * next：中间状态
     * end：结束状态
     */
    //5.1：定义规则模型（静态的）
    val  loginEventPattern: Pattern[LoginEvent, LoginEvent] = Pattern.begin[LoginEvent]("begin")
      .where(_.eventType == "fail") //匹配第一个事件，匹配的是登录失败
      .next("next") //在前一个事件的基础上，又跟上了一个时间，next表示两个时间之间必须是严格临近的
      .where(_.eventType == "success") //匹配第二个事件，匹配的是登录失败
      .within(Time.seconds(3)) //定义结束状态，结束状态可以是时间触发也可以是某种事件触发

    //5.2：将规则模型应用到数据流中，进行匹配数据
    val patternDataStream: PatternStream[LoginEvent] = CEP.pattern(loginEventStream.keyBy(_.userId), loginEventPattern)

    //5.3：拿到获取到的符合规则的数据
    //定义传入值类型：LoginEvent
    //定义返回值类型：（用户id， 登录IP，登录状态）
    val loginFailDataStream: DataStream[(Long, String, String, Long)] = patternDataStream.select(new PatternSelectFunction[LoginEvent, (Long, String, String, Long)] {
      //将匹配到的数据查询出来返回
      override def select(pattern: util.Map[String, util.List[LoginEvent]]): (Long, String, String, Long) = {
        //根据刚才的分析，已经根据状态条件将符合规则的数据放到了state里面，所以查找匹配好的数据可以在state里面找到
        val loginEvent = pattern.getOrDefault("next", null).iterator().next()
        //返回匹配到的数据
        (loginEvent.userId, loginEvent.ip, loginEvent.eventType, loginEvent.eventTime)
      }
    })

    //6：打印符合条件的数据
    loginFailDataStream.printToErr("连续两次登录失败的用户>>>")

    //7：启动任务
    env.execute()
  }
}
