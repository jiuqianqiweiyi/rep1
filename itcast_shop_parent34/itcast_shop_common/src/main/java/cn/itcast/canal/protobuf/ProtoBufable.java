package cn.itcast.canal.protobuf;


/**
 * ProtoBuf序列化接口
 * 这个接口中定义的是返回bytes[]这个对象
 * 所有的能够使用ProtoBuf序列化的bean都需要实现该接口
 */
public interface ProtoBufable {
    /**
     * 将对象转换成字节数组
     * @return 字节数组
     */
    byte[] toByte();
}
