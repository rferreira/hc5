package org.ophion.hc5;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.BasicRequestConsumer;
import org.apache.hc.core5.http.nio.BasicResponseProducer;
import org.apache.hc.core5.http.nio.support.AbstractServerExchangeHandler;
import org.apache.hc.core5.http.nio.support.classic.AbstractClassicEntityConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2ServerBootstrap;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int port = 8080;

        final IOReactorConfig config = IOReactorConfig.DEFAULT;

        final H2Config h2Config = H2Config.DEFAULT;

        final HttpAsyncServer server = H2ServerBootstrap.bootstrap()
                .setH2Config(h2Config)
                .setIOReactorConfig(config)
                .setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
                .register("*", CustomServerExchangeHandler::new)
                .create();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("HTTP server shutting down");
            server.close(CloseMode.GRACEFUL);
        }));
        server.start();
        final Future<ListenerEndpoint> future = server.listen(new InetSocketAddress(port));
        final ListenerEndpoint listenerEndpoint = future.get();
        System.out.println("Listening on " + listenerEndpoint.getAddress());
        server.awaitShutdown(TimeValue.ofDays(Long.MAX_VALUE));
    }

    static class CustomServerExchangeHandler extends AbstractServerExchangeHandler<Message<HttpRequest, HttpEntity>> {


        @Override
        protected AsyncRequestConsumer<Message<HttpRequest, HttpEntity>> supplyConsumer(HttpRequest request,
                                                                                        EntityDetails
                                                                                                entityDetails,
                                                                                        HttpContext context) {
            return new BasicRequestConsumer<>(new AbstractClassicEntityConsumer<HttpEntity>(1024 * 64, ForkJoinPool.commonPool()) {
                @Override
                protected HttpEntity consumeData(ContentType contentType, InputStream inputStream) {
                    return new InputStreamEntity(inputStream, entityDetails.getContentLength(), contentType);
                }
            });

        }

        @Override
        protected void handle(Message<HttpRequest, HttpEntity> requestMessage,
                              AsyncServerRequestHandler.ResponseTrigger responseTrigger,
                              HttpContext context) throws HttpException, IOException {

            HttpResponse resp = new BasicClassicHttpResponse(200, "hello world");

            final HttpCoreContext coreContext = HttpCoreContext.adapt(context);
            final EndpointDetails endpoint = coreContext.getEndpointDetails();
            final HttpRequest req = requestMessage.getHead();

            System.out.println(String.format("[%s] %s %s %s", Instant.now(),
                    endpoint.getRemoteAddress().toString(),
                    req.getMethod(),
                    req.getPath()));

            responseTrigger.submitResponse(new BasicResponseProducer(resp), context);

            requestMessage.getBody().close();
        }
    }


}
