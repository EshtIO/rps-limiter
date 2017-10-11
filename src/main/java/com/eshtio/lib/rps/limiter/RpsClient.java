package com.eshtio.lib.rps.limiter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Реализация - прототип.
 * TODO: В плане чистоты кода косяков много - понятно, интересует корректность самого подхода
 */
public class RpsClient {

    // Сообщение - маркер для передачи в очередь, чтобы фоновый поток понял, что больше сообщений нет,
    // Добавил для удобства остановки приложения, чтобы приложение завершалось корректно,
    // только всех сообщений уже внутри очереди,
    // а так же после обработки всех повисших сообщений (ожидающих попасть в очередь)
    private static final String STOP_MESSAGE = new Object().toString();

    /**
     * Константа для обозначение времени в 1000 милисекунд (1 секунду)
     */
    private static final long ONE_SECOND_MILLS = 1000;

    private final int rps;
    private final BlockingQueue<String> messagesQueue;
    private final MessageConsumer messageConsumer;

    public RpsClient(int rps) {
        this.rps = rps;
        this.messagesQueue = new ArrayBlockingQueue<>(rps, true);
        messageConsumer = new MessageConsumer();
    }

    public void startConsumer() {
        messageConsumer.start();
    }

    public void waitStopConsumer() {
        // Для ожидания остановки добавляем стоп-слово,
        // как-только до него дойдет очередь, consumer (его поток) завершит свою работу
        putMessage(STOP_MESSAGE);
    }

    /**
     * Основной метод отправки сообщения (запроса).
     * Кладет сообщение в очередь, где сообщение ожидает обработки
     *
     * @param message сообщение - запрос
     */
    public void sendMessage(String message) {
        putMessage(message);
    }

    private void putMessage(String message) {
        try {
            messagesQueue.put(message);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String takeMessage() {
        try {
            return messagesQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Потребитель сообщений для обработки (по легенде отправки на сервис с ограниченным rps)
     */
    private class MessageConsumer extends Thread {

        private int requestCount;
        private long requestCountDuration;

        @Override
        public void run() {
            // Задаем первоначальное состояние при запуске потока
            requestCount = 0;
            requestCountDuration = System.currentTimeMillis();

            while (!isInterrupted()) {
                String message = takeMessage();

                // Если в очередь добавили стоп-слово, то прекращаем обработку.
                // В общем добавил пока только для удобства запуска и остановки main-потока
                // (чтобы main-поток не висел в вечном ожидании новых сообщений и
                // в тоже время успешно обработал все добавленные в очередь сообщения)
                if (STOP_MESSAGE.equals(message)) {
                    return;
                }

                if (requestCount >= rps) {
                    // Если количество обработанных сообщений уже превысило допустимую норму,
                    // а времени с момента последнего обнуления счетчиков прошло меньше секунды
                    // (т.е. если мы превышаем значение request per second)
                    if (System.currentTimeMillis() - requestCountDuration < ONE_SECOND_MILLS) {

                        // считаем, сколько нам нужно подождать (до новой секунды), чтобы отправить очередной запрос
                        long duration = System.currentTimeMillis() - requestCountDuration;
                        System.out.println("Sleep " + (ONE_SECOND_MILLS - duration) + " ms");

                        // ВНИМАНИЕ! Вот тут делаю слип, интересен по большей части именно этот момент.
                        // Задержка минимальная конечно, но все же она есть. Вопрос, критична ли подобная задержка?
                        // Если можно как-то сделать без нее, стоит послать меня еще подумать значит)
                        sleepWithoutException(ONE_SECOND_MILLS - duration);
                    }
                    System.out.println("Reset counters");
                    requestCount = 0;
                    requestCountDuration = System.currentTimeMillis();
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
