package org.code.adsk.sample;

import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.code.adsk.sample.commodity.CommodityOuterClass;
import org.code.adsk.sample.commodity.CommodityServiceGrpc;
import org.code.adsk.sample.order.OrderManagementGrpc;
import org.code.adsk.sample.order.OrderManagementOuterClass;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author xzwang
 * @date 2022/5/11
 */
public class ClientApplication {


    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();

        //CommodityServiceGrpc.CommodityServiceBlockingStub commodityServiceBlockingStub = CommodityServiceGrpc.newBlockingStub(channel);
        //invokeAddAndGetCommodity(commodityServiceBlockingStub);

        //OrderManagementGrpc.OrderManagementBlockingStub orderManagementBlockingStub = OrderManagementGrpc.newBlockingStub(channel);
        OrderManagementGrpc.OrderManagementStub asyncStub = OrderManagementGrpc.newStub(channel);

        //StringValue request = StringValue.of("106");
        //OrderManagementOuterClass.Order order = orderManagementBlockingStub.getOrder(request);
        //System.out.println("Order:" + order);

        //invokeSearchOrders(orderManagementBlockingStub);
        invokeOrderProcess(asyncStub);

        channel.shutdown();
    }

    private static void invokeAddAndGetCommodity(CommodityServiceGrpc.CommodityServiceBlockingStub commodityServiceBlockingStub){
        CommodityOuterClass.Commodity commodity = CommodityOuterClass.Commodity.newBuilder().setName("Samsung S10")
                .setDesc("Samsung Galaxy is the latest smart phone, launched in February 2019.")
                .build();
        CommodityOuterClass.CommodityId commodityId = commodityServiceBlockingStub.addCommodity(commodity);
        System.out.println("CommodityId: " + commodityId.getValue() + " add successfully.");
        CommodityOuterClass.Commodity commodity1 = commodityServiceBlockingStub.getCommodity(commodityId);
        System.out.println("Commodity: " + commodity1.toString());
    }

    private static void invokeSearchOrders(OrderManagementGrpc.OrderManagementBlockingStub orderManagementBlockingStub){
        //匹配中文
        StringValue searchCondition = StringValue.newBuilder().setValue("小米11").build();
        Iterator<OrderManagementOuterClass.Order> orderIterator = orderManagementBlockingStub.searchOrders(searchCondition);
        while (orderIterator.hasNext()){
            OrderManagementOuterClass.Order matchOrder = orderIterator.next();
            System.out.println(matchOrder);
        }
        //未匹配
        searchCondition = StringValue.newBuilder().setValue("小米8").build();
        orderIterator = orderManagementBlockingStub.searchOrders(searchCondition);
        while (orderIterator.hasNext()){
            OrderManagementOuterClass.Order matchOrder = orderIterator.next();
            System.out.println(matchOrder);
        }
        //匹配多条
        searchCondition = StringValue.newBuilder().setValue("Apple").build();
        orderIterator = orderManagementBlockingStub.searchOrders(searchCondition);
        while (orderIterator.hasNext()){
            OrderManagementOuterClass.Order matchOrder = orderIterator.next();
            System.out.println(matchOrder);
        }
    }


    private static void invokeOrderProcess(OrderManagementGrpc.OrderManagementStub asyncStub){
        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<OrderManagementOuterClass.CombinedShipment> orderProcessResponseObserver = new StreamObserver<OrderManagementOuterClass.CombinedShipment>() {
            @Override
            public void onNext(OrderManagementOuterClass.CombinedShipment value) {
                System.out.println("Combined Shipment:" + value.getId() + ":" + value.getOrderListList());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                System.out.println("Order Processing completed!");
                finishLatch.countDown();
            }
        };

        StreamObserver<StringValue> orderProcessRequestObserver = asyncStub.processOrders(orderProcessResponseObserver);

        orderProcessRequestObserver.onNext(StringValue.newBuilder().setValue("104").build());
        orderProcessRequestObserver.onNext(StringValue.newBuilder().setValue("106").build());
        orderProcessRequestObserver.onNext(StringValue.newBuilder().setValue("102").build());
        orderProcessRequestObserver.onNext(StringValue.newBuilder().setValue("103").build());
        //orderProcessRequestObserver.onNext(StringValue.newBuilder().setValue("101").build());

        if (finishLatch.getCount() == 0){
            System.err.println("rpc completed or errored before we finish sending.");
            return;
        }

        orderProcessRequestObserver.onCompleted();

        try {
            if (!finishLatch.await(120, TimeUnit.SECONDS)){
                System.err.println("FAILED: process order cannot finish within 120 second.");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
