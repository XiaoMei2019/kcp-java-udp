以下关于我的问题介绍哦：

1.我的服务器是想在不设置超时时间的时候，一直启动，但是客户端完成一次传输后报错，无法第二次发送数据（错误代发生在 KcpServer类 162行代码，result！=0）
2.我是想把服务器收到的消息发送回去客户端，[加了KcpServer类 178行代码]，数据出错，无法传输成功，我怀疑是串包了？？自己认为哈哈，重点是如何解决。
3.ClientTest中，如果我循环发送的次数较大，比如2000次，数据传不完为啥呢？如何解决？
4.在KcpServer的start方法中我先启动UDP接受数据线程，然后再启动KcpServer这个线程，这两个线程都用到了公用队列rcv_byte_que，send_byte_que，我不知道如何写线程安全，，我用了一个Object的wait方法，这里可能是出错的原因。

5.之后是想实现多个客户端，理想情况是十万个客户端和一个服务器交互，这个要如何设计呢？



希望大神多指教，新手上路错误较多，哈哈，非常感谢。