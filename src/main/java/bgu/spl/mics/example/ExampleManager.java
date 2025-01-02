package bgu.spl.mics.example;

import bgu.spl.mics.example.services.ExampleBroadcastListenerService;
import bgu.spl.mics.example.services.ExampleMessageSenderService;
import bgu.spl.mics.example.services.ExampleEventHandlerService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class ExampleManager {

    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1); // Example latch for synchronization

        Map<String, ServiceCreator> serviceCreators = new HashMap<>();
        serviceCreators.put("ev-handler", (id, params) -> new ExampleEventHandlerService(id, params, latch));
        serviceCreators.put("brod-listener", (id, params) -> new ExampleBroadcastListenerService(id, params, latch));
        serviceCreators.put("sender", (id, params) -> new ExampleMessageSenderService(id, params, latch));

        Scanner sc = new Scanner(System.in);
        boolean quit = false;
        try {
            System.out.println("Example manager is started - supported commands are: start,quit");
            System.out.println("Supporting services: " + serviceCreators.keySet());
            while (!quit) {

                String line = sc.nextLine();
                String[] params = line.split("\\s+");

                if (params.length > 0) {

                    switch (params[0]) {
                        case "start":
                            try {
                                if (params.length < 3) {
                                    throw new IllegalArgumentException("Expecting service type and id, supported types: " + serviceCreators.keySet());
                                }
                                ServiceCreator creator = serviceCreators.get(params[1]);
                                if (creator == null) {
                                    throw new IllegalArgumentException("unknown service type, supported types: " + serviceCreators.keySet());
                                }

                                new Thread(creator.create(params[2], Arrays.copyOfRange(params, 3, params.length))).start();
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Error: " + ex.getMessage());
                            }

                            break;
                        case "quit":
                            quit = true;
                            break;
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("Unexpected Error!!!!");
            t.printStackTrace();
        } finally {
            System.out.println("Manager Terminating - UNGRACEFULLY!");
            sc.close();
            System.exit(0);
        }
    }
}
