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

import bisq.proto.grpc.AddConnectionRequest;
import bisq.proto.grpc.AddConnectionResponse;
import bisq.proto.grpc.MoneroConnectionsGrpc;

import io.grpc.stub.StreamObserver;

import javax.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;

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
            URI uri = new URI(request.getUri());
            String username = nullIfEmpty(request.getUsername());
            String password = nullIfEmpty(request.getPassword());
            coreApi.addConnection(uri, username, password, request.getPriority());
            var reply = AddConnectionResponse.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (URISyntaxException cause) {
            // TODO: invalid URI provided
            exceptionHandler.handleException(log, cause, responseObserver);
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    private static String nullIfEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }
}
