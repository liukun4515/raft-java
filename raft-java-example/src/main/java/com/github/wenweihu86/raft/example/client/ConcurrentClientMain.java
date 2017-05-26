package com.github.wenweihu86.raft.example.client;

import com.github.wenweihu86.raft.example.server.service.Example;
import com.github.wenweihu86.raft.example.server.service.ExampleService;
import com.google.protobuf.util.JsonFormat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by wenweihu86 on 2017/5/14.
 */
public class ConcurrentClientMain {
    private static JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

    public static void main(String[] args) {
        // parse args
        String ipPorts = args[0];

        long startTime = System.currentTimeMillis();
        // set
        ExecutorService writeThreadPool = Executors.newFixedThreadPool(3);
        Future<?>[] future = new Future[3];
        for (int i = 0; i < 3; i++) {
            ExampleService exampleService = new ExampleServiceProxy(ipPorts);
            future[i] = writeThreadPool.submit(new SetTask(exampleService, i));
        }

        while (!future[0].isDone() || !future[1].isDone() || !future[2].isDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        System.out.printf("write elapseMS=%d\n", System.currentTimeMillis() - startTime);

        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // get
        startTime = System.currentTimeMillis();
        ExecutorService readThreadPool = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            ExampleService exampleService = new ExampleServiceProxy(ipPorts);
            future[i] = readThreadPool.submit(new GetTask(exampleService, i));
        }

        while (!future[0].isDone() || !future[1].isDone() || !future[2].isDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        System.out.printf("read elapseMS=%d\n", System.currentTimeMillis() - startTime);
    }

    public static class SetTask implements Runnable {
        private ExampleService exampleService;
        private int id;

        public SetTask(ExampleService exampleService, int id) {
            this.exampleService = exampleService;
            this.id = id;
        }

        @Override
        public void run() {
            for (int j = 0; j < 100000; j++) {
                String key = "hello" + id + j;
                String value = "world" + id + j;
                Example.SetRequest setRequest = Example.SetRequest.newBuilder()
                        .setKey(key).setValue(value).build();
                Example.SetResponse setResponse = exampleService.set(setRequest);
                try {
                    System.out.printf("set request, key=%s value=%s response=%s\n",
                            key, value, printer.print(setResponse));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static class GetTask implements Runnable {
        private ExampleService exampleService;
        private int id;

        public GetTask(ExampleService exampleService, int id) {
            this.exampleService = exampleService;
            this.id = id;
        }

        @Override
        public void run() {
            for (int j = 0; j < 100000; j++) {
                String key = "hello" + id + j;
                Example.GetRequest getRequest = Example.GetRequest.newBuilder()
                        .setKey(key).build();
                Example.GetResponse getResponse = exampleService.get(getRequest);
                try {
                    System.out.printf("get request, key=%s, response=%s\n",
                            key, printer.print(getResponse));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
