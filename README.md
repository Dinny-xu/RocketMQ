### RocketMQ 社区版消息收发说明

新建一个同步发送者

- ProducerGroup: test-a
- Topic: MyTopicA
- Tag: tag1

新建一个同步接收者

- ConsumerGroup: test-a

- Topic: MyTopicA

- Tag: tag1

同步发送，同步接收,指定sharding Key, 消费时线程一致表示同步

新建一个异步发送者

- ProducerGroup: test-a
- Topic: MyTopicA
- Tag: tag1

新建一个异步接收者

- ConsumerGroup: test-a

- Topic: MyTopicA

- Tag: tag1

异步发送,异步接收 线程ID不一致,多个线程并发消费

观察同步与异步的 group、topic、tag 是一致的。开源RocketMQ顺序消息时需要注意:

- 同一个Group ID只对应一种类型的Topic，即不同时用于顺序消息和无序消息的收发。

此时若只开启同步发送、同步消费, 是有顺序性的,但是同步发送、异步同样可以消费, 表示顺序和异步并没有完全隔离,在同步与异步之间, 最好是采用不同的topic 来区分





  