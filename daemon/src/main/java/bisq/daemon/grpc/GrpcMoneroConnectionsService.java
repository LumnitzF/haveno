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
import bisq.core.xmr.model.XmrDaemonConnection;

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
import bisq.proto.grpc.MoneroConnectionsGrpc;
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

import io.grpc.stub.StreamObserver;

import javax.inject.Inject;

import java.time.Duration;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class GrpcMoneroConnectionsService extends MoneroConnectionsGrpc.MoneroConnectionsImplBase {

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
        try {
            coreApi.addConnection(toXmrDaemonConnection(request.getConnection()));
            var reply = AddConnectionResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (URISyntaxException cause) {
            handleUriSyntaxException(responseObserver, cause);
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void removeConnection(RemoveConnectionRequest request,
                                 StreamObserver<RemoveConnectionResponse> responseObserver) {
        try {
            coreApi.removeConnection(toURI(request.getUri()));
            var reply = RemoveConnectionResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (URISyntaxException cause) {
            handleUriSyntaxException(responseObserver, cause);
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void getConnection(GetConnectionRequest request,
                              StreamObserver<GetConnectionResponse> responseObserver) {
        try {
            ResponseUriConnection responseConnection = toResponseUriConnection(coreApi.getConnection());
            var reply = GetConnectionResponse.newBuilder().setConnection(responseConnection).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void getConnections(GetConnectionsRequest request,
                               StreamObserver<GetConnectionsResponse> responseObserver) {
        try {
            List<XmrDaemonConnection> connections = coreApi.getConnections();
            List<ResponseUriConnection> responseConnections = connections.stream().map(GrpcMoneroConnectionsService::toResponseUriConnection).collect(Collectors.toList());
            var reply = GetConnectionsResponse.newBuilder().addAllConnections(responseConnections).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void setConnection(SetConnectionRequest request,
                              StreamObserver<SetConnectionResponse> responseObserver) {
        try {
            coreApi.setConnection(toURI(request.getUri()));
            var reply = SetConnectionResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (URISyntaxException cause) {
            handleUriSyntaxException(responseObserver, cause);
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void extendedSetConnection(ExtendedSetConnectionRequest request,
                                      StreamObserver<ExtendedSetConnectionResponse> responseObserver) {
        try {
            coreApi.setConnection(toXmrDaemonConnection(request.getConnection()));
            var reply = ExtendedSetConnectionResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (URISyntaxException cause) {
            handleUriSyntaxException(responseObserver, cause);
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void checkCurrentConnection(CheckCurrentConnectionRequest request,
                                       StreamObserver<CheckCurrentConnectionResponse> responseObserver) {
        try {
            XmrDaemonConnection connection = coreApi.checkConnection();
            var reply = CheckCurrentConnectionResponse.newBuilder()
                    .setConnection(toResponseUriConnection(connection)).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void checkConnection(CheckConnectionRequest request,
                                StreamObserver<CheckConnectionResponse> responseObserver) {
        try {
            XmrDaemonConnection connection = coreApi.checkConnection(toXmrDaemonConnection(request.getConnection()));
            var reply = CheckConnectionResponse.newBuilder()
                    .setConnection(toResponseUriConnection(connection)).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (URISyntaxException cause) {
            handleUriSyntaxException(responseObserver, cause);
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void checkConnections(CheckConnectionsRequest request,
                                 StreamObserver<CheckConnectionsResponse> responseObserver) {
        try {
            List<XmrDaemonConnection> connections = coreApi.checkConnections();
            List<ResponseUriConnection> responseConnections = connections.stream()
                    .map(GrpcMoneroConnectionsService::toResponseUriConnection).collect(Collectors.toList());
            var reply = CheckConnectionsResponse.newBuilder().addAllConnections(responseConnections).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void startCheckingConnections(StartCheckingConnectionsRequest request,
                                         StreamObserver<StartCheckingConnectionsResponse> responseObserver) {
        try {
            int refreshMillis = request.getRefreshPeriod();
            Duration refreshPeriod = refreshMillis == 0 ? null : Duration.ofMillis(refreshMillis);
            coreApi.startCheckingConnection(refreshPeriod);
            var reply = StartCheckingConnectionsResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void stopCheckingConnections(StopCheckingConnectionsRequest request,
                                        StreamObserver<StopCheckingConnectionsResponse> responseObserver) {
        try {
            coreApi.stopCheckingConnection();
            var reply = StopCheckingConnectionsResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void getBestAvailableConnection(GetBestAvailableConnectionRequest request,
                                           StreamObserver<GetBestAvailableConnectionResponse> responseObserver) {
        try {
            XmrDaemonConnection connection = coreApi.getBestAvailableConnection();
            var reply = GetBestAvailableConnectionResponse.newBuilder()
                    .setConnection(toResponseUriConnection(connection)).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    @Override
    public void setAutoSwitch(SetAutoSwitchRequest request,
                              StreamObserver<SetAutoSwitchResponse> responseObserver) {
        try {
            coreApi.setAutoSwitch(request.getAutoSwitch());

            var reply = SetAutoSwitchResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            handleGenericException(responseObserver, cause);
        }
    }

    private void handleUriSyntaxException(StreamObserver<?> responseObserver, URISyntaxException cause) {
        // TODO: Different error handling?
        handleGenericException(responseObserver, cause);
    }

    private void handleGenericException(StreamObserver<?> responseObserver, Throwable cause) {
        exceptionHandler.handleException(log, cause, responseObserver);
    }

    private static ResponseUriConnection toResponseUriConnection(XmrDaemonConnection xmrDaemonConnection) {
        return ResponseUriConnection.newBuilder()
                .setUri(xmrDaemonConnection.getUri().toString())
                .setPriority(xmrDaemonConnection.getPriority())
                .setIsOnline(xmrDaemonConnection.isOnline())
                .setAuthenticated(toAuthenticationStatus(xmrDaemonConnection.getAuthenticationStatus()))
                .build();
    }

    private static ResponseUriConnection.AuthenticationStatus toAuthenticationStatus(XmrDaemonConnection.AuthenticationStatus authenticationStatus) {
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

    private static XmrDaemonConnection toXmrDaemonConnection(RequestUriConnection connection) throws URISyntaxException {
        return XmrDaemonConnection.builder()
                .uri(toURI(connection.getUri()))
                .priority(connection.getPriority())
                .username(nullIfEmpty(connection.getUsername()))
                .password(nullIfEmpty(connection.getPassword()))
                .build();
    }

    private static URI toURI(String uri) throws URISyntaxException {
        if (uri.isEmpty()) {
            throw new IllegalArgumentException("URI is required");
        }
        return new URI(uri);
    }

    private static String nullIfEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }
}
