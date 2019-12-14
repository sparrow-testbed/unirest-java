/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.unirest.apache;

import kong.unirest.*;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

public class ApacheAsyncClient extends BaseApacheClient implements AsyncClient {

    private ApacheAsyncConfig apache;


    public ApacheAsyncClient(Config config) {
        this.apache = new ApacheAsyncConfig(config);
    }


    public ApacheAsyncClient(CloseableHttpAsyncClient client, Config config) {
        this.apache = new ApacheAsyncConfig(client, config);
    }

    @Override
    public void registerShutdownHook() {
        this.apache.registerShutdownHook();
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> request(
            HttpRequest request,
            Function<RawResponse, HttpResponse<T>> transformer,
            CompletableFuture<HttpResponse<T>> callback) {

        Objects.requireNonNull(callback);


        SimpleHttpRequest requestObj = new RequestPrep(request, apache.config, true).prepareSimple();
        HttpRequestSummary reqSum = request.toSummary();
        HttpHost host = determineTarget(request, request.getHeaders());
        MetricContext metric = apache.config.getMetric().begin(reqSum);
        HttpContext context = configFactory.apply(apache.config, request);

//        protected abstract <T> Future<T> doExecute(
//        final HttpHost target,
//        final AsyncRequestProducer requestProducer,
//        final AsyncResponseConsumer<T> responseConsumer,
//        final HandlerFactory<AsyncPushConsumer> pushHandlerFactory,
//        final HttpContext context,
//        final FutureCallback<T> callback);
        apache.client.execute(host,
                new UniRequestProducer(),
                new UniResponseConsumer(),
                new UniHandlerFactory(),
                context,
                new FutureCallback<SimpleHttpResponse>() {
            @Override
            public void completed(SimpleHttpResponse httpResponse) {
                ApacheAsyncResponse t = new ApacheAsyncResponse(httpResponse, apache.config);
                HttpResponseSummary rezSum = t.toSummary();
                HttpResponse<T> response = transformBody(transformer, t);
                metric.complete(rezSum, null);
                apache.config.getUniInterceptor().onResponse(response, reqSum, apache.config);
                HttpResponse<T> value = transformBody(transformer, t);
                callback.complete(value);
            }

            @Override
            public void failed(Exception e) {
                metric.complete(null, e);
                try {
                    HttpResponse r = apache.config.getUniInterceptor().onFail(e, null, apache.config);
                    callback.complete(r);
                } catch (Exception ee){
                    callback.completeExceptionally(e);
                }
            }

            @Override
            public void cancelled() {
                UnirestException canceled = new UnirestException("canceled");
                metric.complete(null, canceled);
                callback.completeExceptionally(canceled);
                apache.config.getUniInterceptor().onFail(canceled, reqSum, apache.config);
            }
        });
        return callback;
    }

    @Override
    public HttpAsyncClient getClient() {
        return apache.getClient();
    }

    @Override
    public Stream<Exception> close() {
        return apache.close();
    }

    public static Builder builder(CloseableHttpAsyncClient client) {
        return new Builder(client);
    }

    public static class Builder implements Function<Config, AsyncClient> {
        private CloseableHttpAsyncClient asyncClient;
        private RequestConfigFactory cf;

        public Builder(CloseableHttpAsyncClient client) {
            this.asyncClient = client;
        }

        @Override
        public AsyncClient apply(Config config) {
            ApacheAsyncClient client = new ApacheAsyncClient(this.asyncClient, config);
            if (cf != null) {
                client.setConfigFactory(cf);
            }
            return client;
        }

        public Builder withRequestConfig(RequestConfigFactory factory) {
            Objects.requireNonNull(factory);
            this.cf = factory;
            return this;
        }
    }
}
