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

import bisq.proto.grpc.AddConnectionRequest;
import bisq.proto.grpc.AddConnectionResponse;
import bisq.proto.grpc.CheckConnectionRequest;
import bisq.proto.grpc.CheckConnectionResponse;
import bisq.proto.grpc.CheckConnectionsRequest;
import bisq.proto.grpc.CheckConnectionsResponse;
import bisq.proto.grpc.CheckCurrentConnectionRequest;
import bisq.proto.grpc.CheckCurrentConnectionResponse;
import bisq.proto.grpc.ExtendedSetConnectionRequest;
import bisq.proto.grpc.ExtendedSetConnectionResponse;
import bisq.proto.grpc.GetBestAvailableConnectionRequest;
import bisq.proto.grpc.GetBestAvailableConnectionResponse;
import bisq.proto.grpc.GetConnectionRequest;
import bisq.proto.grpc.GetConnectionResponse;
import bisq.proto.grpc.GetConnectionsRequest;
import bisq.proto.grpc.GetConnectionsResponse;
import bisq.proto.grpc.RemoveConnectionRequest;
import bisq.proto.grpc.RemoveConnectionResponse;
import bisq.proto.grpc.RequestUriConnection;
import bisq.proto.grpc.ResponseUriConnection;
import bisq.proto.grpc.SetAutoSwitchRequest;
import bisq.proto.grpc.SetAutoSwitchResponse;
import bisq.proto.grpc.SetConnectionRequest;
import bisq.proto.grpc.SetConnectionResponse;
import bisq.proto.grpc.StartCheckingConnectionsRequest;
import bisq.proto.grpc.StartCheckingConnectionsResponse;
import bisq.proto.grpc.StopCheckingConnectionsRequest;
import bisq.proto.grpc.StopCheckingConnectionsResponse;

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
                              StreamObserver<AddConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.addMoneroConnection(toUriConnection(request.getConnection()));
            return AddConnectionResponse.newBuilder().build();
        });
    }

    @Override
    public void removeConnection(RemoveConnectionRequest request,
                                 StreamObserver<RemoveConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.removeMoneroConnection(validateUri(request.getUri()));
            return RemoveConnectionResponse.newBuilder().build();
        });
    }

    @Override
    public void getConnection(GetConnectionRequest request,
                              StreamObserver<GetConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            ResponseUriConnection responseConnection = toResponseUriConnection(coreApi.getMoneroConnection());
            return GetConnectionResponse.newBuilder().setConnection(responseConnection).build();
        });
    }

    @Override
    public void getConnections(GetConnectionsRequest request,
                               StreamObserver<GetConnectionsResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            List<UriConnection> connections = coreApi.getMoneroConnections();
            List<ResponseUriConnection> responseConnections = connections.stream().map(GrpcMoneroConnectionsService::toResponseUriConnection).collect(Collectors.toList());
            return GetConnectionsResponse.newBuilder().addAllConnections(responseConnections).build();
        });
    }

    @Override
    public void setConnection(SetConnectionRequest request,
                              StreamObserver<SetConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.setMoneroConnection(validateUri(request.getUri()));
            return SetConnectionResponse.newBuilder().build();
        });
    }

    @Override
    public void extendedSetConnection(ExtendedSetConnectionRequest request,
                                      StreamObserver<ExtendedSetConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.setMoneroConnection(toUriConnection(request.getConnection()));
            return ExtendedSetConnectionResponse.newBuilder().build();

        });
    }

    @Override
    public void checkCurrentConnection(CheckCurrentConnectionRequest request,
                                       StreamObserver<CheckCurrentConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            UriConnection connection = coreApi.checkMoneroConnection();
            return CheckCurrentConnectionResponse.newBuilder()
                    .setConnection(toResponseUriConnection(connection)).build();
        });
    }

    @Override
    public void checkConnection(CheckConnectionRequest request,
                                StreamObserver<CheckConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            UriConnection connection = coreApi.checkMoneroConnection(toUriConnection(request.getConnection()));
            return CheckConnectionResponse.newBuilder()
                    .setConnection(toResponseUriConnection(connection)).build();
        });
    }

    @Override
    public void checkConnections(CheckConnectionsRequest request,
                                 StreamObserver<CheckConnectionsResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            List<UriConnection> connections = coreApi.checkMoneroConnections();
            List<ResponseUriConnection> responseConnections = connections.stream()
                    .map(GrpcMoneroConnectionsService::toResponseUriConnection).collect(Collectors.toList());
            return CheckConnectionsResponse.newBuilder().addAllConnections(responseConnections).build();
        });
    }

    @Override
    public void startCheckingConnections(StartCheckingConnectionsRequest request,
                                         StreamObserver<StartCheckingConnectionsResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            int refreshMillis = request.getRefreshPeriod();
            Long refreshPeriod = refreshMillis == 0 ? null : (long) refreshMillis;
            coreApi.startCheckingMoneroConnection(refreshPeriod);
            return StartCheckingConnectionsResponse.newBuilder().build();
        });
    }

    @Override
    public void stopCheckingConnections(StopCheckingConnectionsRequest request,
                                        StreamObserver<StopCheckingConnectionsResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.stopCheckingMoneroConnection();
            return StopCheckingConnectionsResponse.newBuilder().build();
        });
    }

    @Override
    public void getBestAvailableConnection(GetBestAvailableConnectionRequest request,
                                           StreamObserver<GetBestAvailableConnectionResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            UriConnection connection = coreApi.getBestAvailableMoneroConnection();
            return GetBestAvailableConnectionResponse.newBuilder()
                    .setConnection(toResponseUriConnection(connection)).build();
        });
    }

    @Override
    public void setAutoSwitch(SetAutoSwitchRequest request,
                              StreamObserver<SetAutoSwitchResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.setMoneroConnectionAutoSwitch(request.getAutoSwitch());
            return SetAutoSwitchResponse.newBuilder().build();
        });
    }

    private <_Response> void handleRequest(StreamObserver<_Response> responseObserver,
                                           RpcRequestHandler<_Response> handler) {
        try {
            _Response response = handler.handleRequest();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (URISyntaxException cause) {
            handleUriSyntaxException(responseObserver, cause);
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @FunctionalInterface
    private interface RpcRequestHandler<_Response> {
        _Response handleRequest() throws Exception;
    }

    private void handleUriSyntaxException(StreamObserver<?> responseObserver, URISyntaxException cause) {
        // TODO: Different error handling?
        handleGenericException(responseObserver, cause);
    }

    private void handleGenericException(StreamObserver<?> responseObserver, Throwable cause) {
        exceptionHandler.handleException(log, cause, responseObserver);
    }

    private static ResponseUriConnection toResponseUriConnection(UriConnection uriConnection) {
        if (uriConnection == null) {
            return null;
        }
        return ResponseUriConnection.newBuilder()
                .setUri(uriConnection.getUri().toString())
                .setPriority(uriConnection.getPriority())
                .setIsOnline(uriConnection.isOnline())
                .setAuthenticated(toAuthenticationStatus(uriConnection.getAuthenticationStatus()))
                .build();
    }

    private static ResponseUriConnection.AuthenticationStatus toAuthenticationStatus(UriConnection.AuthenticationStatus authenticationStatus) {
        switch (authenticationStatus) {
            case NO_AUTHENTICATION:
                return ResponseUriConnection.AuthenticationStatus.NO_AUTHENTICATION;
            case AUTHENTICATED:
                return ResponseUriConnection.AuthenticationStatus.AUTHENTICATED;
            case NOT_AUTHENTICATED:
                return ResponseUriConnection.AuthenticationStatus.NOT_AUTHENTICATED;
            default:
                throw new UnsupportedOperationException(String.format("Unsupported authentication status %s", authenticationStatus));
        }
    }

    private static UriConnection toUriConnection(RequestUriConnection connection) throws URISyntaxException {
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
                            put(getExtendedSetConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
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
