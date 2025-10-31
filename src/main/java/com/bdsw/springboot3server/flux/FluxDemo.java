package com.bdsw.springboot3server.flux;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.io.IOException;
import java.time.Duration;

/**
 * @Author Daniel
 * @Date 2025/10/26/周日 22:05
 * @Description TODO
 **/
public class FluxDemo {

    public static void main(String[] args) throws InterruptedException {

        Flux<Integer> integerFlux = Flux.range(1, 10)
                .delayElements(Duration.ofSeconds(1))
                .doOnSubscribe(subscription ->  System.out.println("订阅者和发布者绑定好了:" + subscription + "\n"))
                .doOnError(throwable -> System.out.printf("流出错了:" + throwable + "\n"))
                .doOnNext(integer -> System.out.println("doOnNext " + integer + "\n"))
                .doOnComplete(() -> System.out.printf("流正常结束\n"))
                .doOnCancel(() -> System.out.printf("流已被取消\n"));


        integerFlux.subscribe(new BaseSubscriber<>() {

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                System.out.println("订阅者和发布者绑定好了...:" + subscription + "\n");
                request(1); // 背压，触发hookOnNext
            }

            @Override
            protected void hookOnNext(Integer integer) {
                System.out.println("元素到达...:" + integer + "\n");
                if (integer < 5) {
                    request(1); // 继续背压
                    if (integer == 3) {
                        int i = 10/0;
                    }
                } else {
                    cancel();
                }
            }

            @Override
            protected void hookOnError(Throwable throwable) {
                System.out.printf("流出错了...:" + throwable + "\n");
            }

            @Override
            protected void hookOnComplete() {
                System.out.printf("流正常结束..." + "\n");
            }

            @Override
            protected void hookFinally(SignalType type) {
                System.out.println("流finally...:" + type + "\n");
                super.hookFinally(type);
            }
        });
        Thread.sleep(20000);
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


        // Mono<Integer> 只有一个Integer
        // Flux<Integer> 有很多Integer
//        Mono<Integer> just = Mono.just(1);
//        just.subscribe(System.out::println);

        // 空流
        // 事件感知：当流发生什么事的时候，触发一个回调；doOnXxx；
//        Flux<Object> empty = Flux.empty() // 有一个信号，此时代表完成信号
//                .doOnComplete(() -> System.out.println("流结束了。。。"));
        // 不订阅，不会发生doOnComplete
//        empty.subscribe(System.out::println);
    }
}
