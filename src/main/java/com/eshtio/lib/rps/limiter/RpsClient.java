package com.eshtio.lib.rps.limiter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class RpsClient {

    private static final long ONE_SECOND_MILLS = 1000;

    private final int rps;
    private final BlockingQueue<String> messages;

    public RpsClient(int rps) {
        this.rps = rps;
        this.messages = new ArrayBlockingQueue<>(rps, true);

        MessageConsumer messageConsumer = new MessageConsumer();
        messageConsumer.start();
    }

    public void sendMessage(String message) {
        putMessage(message);
    }

    private void putMessage(String message) {
        try {
            messages.put(message);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String takeMessage() {
        try {
            return messages.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class MessageConsumer extends Thread {

        private int requestCount;
        private long requestCountDuration;

        @Override
        public void run() {
            requestCount = 0;
            requestCountDuration = System.currentTimeMillis();
            while (!isInterrupted()) {
                String message = takeMessage();

                if (requestCount >= rps) {
                    // Если количество обработанных сообщений уже превысило допустимую норму,
                    // а времени с момента последнего обнуления счетчиков прошло меньше секунды
                    // (т.е. если мы превышаем значение request per second)
                    if (System.currentTimeMillis() - requestCountDuration < ONE_SECOND_MILLS) {
                        // считаем, сколько нам нужно подождать до новой секунды, чтобы отправить запрос
                        long duration = System.currentTimeMillis() - requestCountDuration;
                        System.out.println("Sleep " + (ONE_SECOND_MILLS - duration) + " ms");
                        sleepWithoutException(ONE_SECOND_MILLS - duration);
                    }
                    requestCount = 0;
                    requestCountDuration = System.currentTimeMillis();
                    System.out.println("Reset counters");
                }

                consumeMessage(message);
                requestCount++;
                System.out.println("Request count: " + requestCount);
            }
        }

        private void sleepWithoutException(long mills) {
            try {
                sleep(mills);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private void consumeMessage(String message) {
            // Эмуляция сети
            sleepWithoutException(ThreadLocalRandom.current().nextLong(500));
            System.out.println("Send message: " + message);
        }


    }


}
