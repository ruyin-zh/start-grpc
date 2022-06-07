package org.code.adsk.sample.commodity;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xzwang
 * @date 2022/5/10
 */
public class CommodityServiceImpl extends CommodityServiceGrpc.CommodityServiceImplBase {

    private final Map<String, CommodityOuterClass.Commodity> commodityMap = new ConcurrentHashMap<>();

    @Override
    public void addCommodity(CommodityOuterClass.Commodity request, StreamObserver<CommodityOuterClass.CommodityId> responseObserver) {
        UUID uuid = UUID.randomUUID();
        String randomUuid = uuid.toString();
        commodityMap.put(randomUuid, request);
        CommodityOuterClass.CommodityId commodityId = CommodityOuterClass.CommodityId.newBuilder().setValue(randomUuid).build();
        responseObserver.onNext(commodityId);
        responseObserver.onCompleted();
    }

    @Override
    public void getCommodity(CommodityOuterClass.CommodityId request, StreamObserver<CommodityOuterClass.Commodity> responseObserver) {
        String commodityId = request.getValue();
        if (commodityMap.containsKey(commodityId)){
            responseObserver.onNext(commodityMap.get(commodityId));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
        }
    }
}
