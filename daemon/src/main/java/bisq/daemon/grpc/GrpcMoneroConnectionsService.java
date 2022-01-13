/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.daemon.grpc;

import bisq.core.api.CoreApi;
import bisq.core.api.model.UriConnection;

import bisq.proto.grpc.AddConnectionReply;
import bisq.proto.grpc.AddConnectionRequest;
import bisq.proto.grpc.CheckConnectionReply;
import bisq.proto.grpc.CheckConnectionRequest;
import bisq.proto.grpc.CheckConnectionsReply;
import bisq.proto.grpc.CheckConnectionsRequest;
import bisq.proto.grpc.CheckCurrentConnectionReply;
import bisq.proto.grpc.CheckCurrentConnectionRequest;
import bisq.proto.grpc.GetBestAvailableConnectionReply;
import bisq.proto.grpc.GetBestAvailableConnectionRequest;
import bisq.proto.grpc.GetConnectionReply;
import bisq.proto.grpc.GetConnectionRequest;
import bisq.proto.grpc.GetConnectionsReply;
import bisq.proto.grpc.GetConnectionsRequest;
import bisq.proto.grpc.RemoveConnectionReply;
import bisq.proto.grpc.RemoveConnectionRequest;
import bisq.proto.grpc.SetAutoSwitchReply;
import bisq.proto.grpc.SetAutoSwitchRequest;
import bisq.proto.grpc.SetConnectionReply;
import bisq.proto.grpc.SetConnectionRequest;
import bisq.proto.grpc.StartCheckingConnectionsReply;
import bisq.proto.grpc.StartCheckingConnectionsRequest;
import bisq.proto.grpc.StopCheckingConnectionsReply;
import bisq.proto.grpc.StopCheckingConnectionsRequest;

import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import static bisq.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import static bisq.proto.grpc.MoneroConnectionsGrpc.*;
import static java.util.concurrent.TimeUnit.SECONDS;



import bisq.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import bisq.daemon.grpc.interceptor.GrpcCallRateMeter;

@Slf4j
class GrpcMoneroConnectionsService extends MoneroConnectionsImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcMoneroConnectionsService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void addConnection(AddConnectionRequest request,
                              StreamObserver<AddConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.addMoneroConnection(toInternalUriConnection(request.getConnection()));
            return AddConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void removeConnection(RemoveConnectionRequest request,
                                 StreamObserver<RemoveConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.removeMoneroConnection(validateUri(request.getUri()));
            return RemoveConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void getConnection(GetConnectionRequest request,
                              StreamObserver<GetConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            bisq.proto.grpc.UriConnection replyConnection = toGrpcUriConnection(coreApi.getMoneroConnection());
            return GetConnectionReply.newBuilder().setConnection(replyConnection).build();
        });
    }

    @Override
    public void getConnections(GetConnectionsRequest request,
                               StreamObserver<GetConnectionsReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            List<UriConnection> connections = coreApi.getMoneroConnections();
            List<bisq.proto.grpc.UriConnection> replyConnections = connections.stream()
                    .map(GrpcMoneroConnectionsService::toGrpcUriConnection).collect(Collectors.toList());
            return GetConnectionsReply.newBuilder().addAllConnections(replyConnections).build();
        });
    }

    @Override
    public void setConnection(SetConnectionRequest request,
                              StreamObserver<SetConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            if (!request.hasConnection()) {
                // It is unimportant whether setMoneroConnection(String) or
                // setMoneroConnection(UriConnection) is called, as both will lead to the same result
                // i.e. disconnecting the client
                coreApi.setMoneroConnection((String) null);
            } else {
                coreApi.setMoneroConnection(toInternalUriConnection(request.getConnection()));
            }
            return SetConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void checkCurrentConnection(CheckCurrentConnectionRequest request,
                                       StreamObserver<CheckCurrentConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            UriConnection connection = coreApi.checkMoneroConnection();
            return CheckCurrentConnectionReply.newBuilder()
                    .setConnection(toGrpcUriConnection(connection)).build();
        });
    }

    @Override
    public void checkConnection(CheckConnectionRequest request,
                                StreamObserver<CheckConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            UriConnection connection = coreApi.checkMoneroConnection(toInternalUriConnection(request.getConnection()));
            return CheckConnectionReply.newBuilder()
                    .setConnection(toGrpcUriConnection(connection)).build();
        });
    }

    @Override
    public void checkConnections(CheckConnectionsRequest request,
                                 StreamObserver<CheckConnectionsReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            List<UriConnection> connections = coreApi.checkMoneroConnections();
            List<bisq.proto.grpc.UriConnection> replyConnections = connections.stream()
                    .map(GrpcMoneroConnectionsService::toGrpcUriConnection).collect(Collectors.toList());
            return CheckConnectionsReply.newBuilder().addAllConnections(replyConnections).build();
        });
    }

    @Override
    public void startCheckingConnections(StartCheckingConnectionsRequest request,
                                         StreamObserver<StartCheckingConnectionsReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            int refreshMillis = request.getRefreshPeriod();
            Long refreshPeriod = refreshMillis == 0 ? null : (long) refreshMillis;
            coreApi.startCheckingMoneroConnection(refreshPeriod);
            return StartCheckingConnectionsReply.newBuilder().build();
        });
    }

    @Override
    public void stopCheckingConnections(StopCheckingConnectionsRequest request,
                                        StreamObserver<StopCheckingConnectionsReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.stopCheckingMoneroConnection();
            return StopCheckingConnectionsReply.newBuilder().build();
        });
    }

    @Override
    public void getBestAvailableConnection(GetBestAvailableConnectionRequest request,
                                           StreamObserver<GetBestAvailableConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            UriConnection connection = coreApi.getBestAvailableMoneroConnection();
            return GetBestAvailableConnectionReply.newBuilder()
                    .setConnection(toGrpcUriConnection(connection)).build();
        });
    }

    @Override
    public void setAutoSwitch(SetAutoSwitchRequest request,
                              StreamObserver<SetAutoSwitchReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.setMoneroConnectionAutoSwitch(request.getAutoSwitch());
            return SetAutoSwitchReply.newBuilder().build();
        });
    }

    private <_Reply> void handleRequest(StreamObserver<_Reply> responseObserver,
                                        RpcRequestHandler<_Reply> handler) {
        try {
            _Reply reply = handler.handleRequest();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @FunctionalInterface
    private interface RpcRequestHandler<_Reply> {
        _Reply handleRequest() throws Exception;
    }

    private static bisq.proto.grpc.UriConnection toGrpcUriConnection(UriConnection uriConnection) {
        if (uriConnection == null) {
            return null;
        }
        return bisq.proto.grpc.UriConnection.newBuilder()
                .setUri(uriConnection.getUri())
                .setPriority(uriConnection.getPriority())
                .setIsOnline(uriConnection.isOnline())
                .setAuthenticated(toAuthenticationStatus(uriConnection.getAuthenticationStatus()))
                .build();
    }

    private static bisq.proto.grpc.UriConnection.AuthenticationStatus toAuthenticationStatus(UriConnection.AuthenticationStatus authenticationStatus) {
        switch (authenticationStatus) {
            case NO_AUTHENTICATION:
                return bisq.proto.grpc.UriConnection.AuthenticationStatus.NO_AUTHENTICATION;
            case AUTHENTICATED:
                return bisq.proto.grpc.UriConnection.AuthenticationStatus.AUTHENTICATED;
            case NOT_AUTHENTICATED:
                return bisq.proto.grpc.UriConnection.AuthenticationStatus.NOT_AUTHENTICATED;
            default:
                throw new UnsupportedOperationException(String.format("Unsupported authentication status %s", authenticationStatus));
        }
    }

    private static UriConnection toInternalUriConnection(bisq.proto.grpc.UriConnection connection) throws URISyntaxException {
        return UriConnection.builder()
                .uri(validateUri(connection.getUri()))
                .priority(connection.getPriority())
                .username(nullIfEmpty(connection.getUsername()))
                .password(nullIfEmpty(connection.getPassword()))
                .build();
    }

    private static String validateUri(String uri) throws URISyntaxException {
        if (uri.isEmpty()) {
            throw new IllegalArgumentException("URI is required");
        }
        // Create new URI for validation, internally String is used again
        return new URI(uri).toString();
    }

    private static String nullIfEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }


    final ServerInterceptor[] interceptors() {
        Optional<ServerInterceptor> rateMeteringInterceptor = rateMeteringInterceptor();
        return rateMeteringInterceptor.map(serverInterceptor ->
                new ServerInterceptor[]{serverInterceptor}).orElseGet(() -> new ServerInterceptor[0]);
    }

    private Optional<ServerInterceptor> rateMeteringInterceptor() {
        return getCustomRateMeteringInterceptor(coreApi.getConfig().appDataDir, this.getClass())
                .or(() -> Optional.of(CallRateMeteringInterceptor.valueOf(
                        new HashMap<>() {{
                            put(getAddConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getRemoveConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getGetConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getGetConnectionsMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getSetConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getCheckCurrentConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getCheckConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getCheckConnectionsMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getStartCheckingConnectionsMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getStopCheckingConnectionsMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getGetBestAvailableConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getSetAutoSwitchMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                        }}
                )));
    }
}
