package org.code.adsk.sample;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.code.adsk.sample.commodity.CommodityServiceImpl;
import org.code.adsk.sample.order.OrderManagementImpl;

/**
 * @author xzwang
 * @date 2022/5/10
 */
public class ServerApplication {

    public static void main(String[] args) throws Exception {
        int port = 50051;

        Server server = ServerBuilder.forPort(port)
                .addService(new CommodityServiceImpl())
                .addService(new OrderManagementImpl())
                .build()
                .start();

        System.out.println("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gRPC server since JVM is shutting down.");
            if (server != null){
                server.shutdown();
            }
            System.out.println("Server shutdown");
        }));
        //服务器线程一直等待直到服务器终止
        server.awaitTermination();
    }

}
