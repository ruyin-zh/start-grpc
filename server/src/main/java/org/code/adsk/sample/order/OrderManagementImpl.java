package org.code.adsk.sample.order;

import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xzwang
 * @date 2022/5/13
 */
public class OrderManagementImpl extends OrderManagementGrpc.OrderManagementImplBase {

    private static OrderManagementOuterClass.Order order1 = OrderManagementOuterClass.Order.newBuilder()
            .setId("102")
            .addItems("Google Pixel 3A").addItems("MacBook Pro")
            .setDestination("Mountain View, CA")
            .setPrice(6000)
            .build();

    private static OrderManagementOuterClass.Order order2 = OrderManagementOuterClass.Order.newBuilder()
            .setId("103")
            .addItems("小米11")
            .setDestination("周年限量款")
            .setPrice(2399)
            .build();

    private static OrderManagementOuterClass.Order order3 = OrderManagementOuterClass.Order.newBuilder()
            .setId("104")
            .addItems("华为P20")
            .setDestination("测试数据，勿拍！")
            .setPrice(4999)
            .build();

    private static OrderManagementOuterClass.Order order4 = OrderManagementOuterClass.Order.newBuilder()
            .setId("105")
            .addItems("Apple Watch S4")
            .setDestination("San Jose, CA")
            .setPrice(2400)
            .build();

    private static OrderManagementOuterClass.Order order5 = OrderManagementOuterClass.Order.newBuilder()
            .setId("106")
            .addItems("Amazon Echo").addItems("Apple iPhone XS")
            .setDestination("Mountain View, CA")
            .setPrice(11000)
            .build();

    private static final Map<String, OrderManagementOuterClass.Order> orderMap = new ConcurrentHashMap<>();

    static {
        orderMap.put(order1.getId(),order1);
        orderMap.put(order2.getId(),order2);
        orderMap.put(order3.getId(),order3);
        orderMap.put(order4.getId(),order4);
        orderMap.put(order5.getId(),order5);
    }

    private final Map<String, OrderManagementOuterClass.CombinedShipment> combinedShipmentMap = new HashMap<>();
    private static final int BATCH_SIZE = 3;

    @Override
    public void getOrder(StringValue request, StreamObserver<OrderManagementOuterClass.Order> responseObserver) {
        OrderManagementOuterClass.Order order = orderMap.get(request.getValue());

        if (order != null){
            responseObserver.onNext(order);
        }
        responseObserver.onCompleted();
    }


    @Override
    public void searchOrders(StringValue request, StreamObserver<OrderManagementOuterClass.Order> responseObserver) {
        orderMap.forEach((id, order) -> {
            int itemsCount = order.getItemsCount();
            for (int i = 0; i < itemsCount; i++){
                String item = order.getItems(i);
                if (item.contains(request.getValue())){
                    responseObserver.onNext(order);
                    break;
                }
            }
        });

        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<StringValue> processOrders(StreamObserver<OrderManagementOuterClass.CombinedShipment> responseObserver) {
        return new StreamObserver<StringValue>() {
            int batchMarker = 0;

            @Override
            public void onNext(StringValue value) {
                OrderManagementOuterClass.Order currentOrder = orderMap.get(value.getValue());
                if (currentOrder == null){
                    return;
                }

                batchMarker++;
                String orderDest = currentOrder.getDestination();
                OrderManagementOuterClass.CombinedShipment existingShipment = combinedShipmentMap.get(orderDest);
                if (existingShipment == null){
                    OrderManagementOuterClass.CombinedShipment shipment = OrderManagementOuterClass.CombinedShipment.newBuilder().build();
                    shipment = shipment.newBuilderForType().addOrderList(currentOrder).setId("CMB-" + new Random().nextInt(1000) + ":" + currentOrder.getDestination()).setStatus("Processed!").build();
                    combinedShipmentMap.put(currentOrder.getDestination(), shipment);
                } else {
                    existingShipment = OrderManagementOuterClass.CombinedShipment.newBuilder(existingShipment).addOrderList(currentOrder).build();
                    combinedShipmentMap.put(orderDest, existingShipment);
                }

                if (batchMarker == BATCH_SIZE){
                    combinedShipmentMap.forEach((dest, combinedShip) -> responseObserver.onNext(combinedShip));
                    batchMarker = 0;
                    combinedShipmentMap.clear();
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                combinedShipmentMap.forEach((dest, combinedShip) -> responseObserver.onNext(combinedShip));
                combinedShipmentMap.clear();
                responseObserver.onCompleted();
            }
        };
    }
}
