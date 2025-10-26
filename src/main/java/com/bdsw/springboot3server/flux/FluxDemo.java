package com.bdsw.springboot3server.flux;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

/**
 * @Author Daniel
 * @Date 2025/10/26/周日 22:05
 * @Description TODO
 **/
public class FluxDemo {

    public static void main(String[] args)  {

        // Mono<Integer> 只有一个Integer
        // Flux<Integer> 有很多Integer
        Mono<Integer> just = Mono.just(1);
        just.subscribe(System.out::println);

        // 空流
        // 事件感知：当流发生什么事的时候，触发一个回调；doOnXxx；
        Flux<Object> empty = Flux.empty() // 有一个信号，此时代表完成信号
                .doOnComplete(() -> System.out.println("流结束了。。。"));
        // 不订阅，不会发生doOnComplete
        empty.subscribe(System.out::println);
    }

    public void flux() throws IOException {
        // 1.多元素的流
        Flux<Integer> just = Flux.just(1, 2, 3, 4, 5);

        // 流不消费就没用
        just.subscribe(e -> System.out.println("item=" + e));
        // 一个数据流可以有很多消费者
        just.subscribe(e -> System.out.println("再次消费：item=" + e));

        // 对于每个消费者来说都是一样的； 广播模式；

        System.out.println("-------------");
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

        interval.subscribe(e -> System.out.println("interval=" + e));
        // 控制台不输入不结束
        System.in.read();
    }
}
