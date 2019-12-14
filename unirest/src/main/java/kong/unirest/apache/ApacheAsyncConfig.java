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

import kong.unirest.Config;
import kong.unirest.UnirestConfigException;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.util.TimeValue;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static kong.unirest.apache.BaseApacheClient.toApacheCreds;

class ApacheAsyncConfig {
    final CloseableHttpAsyncClient client;
    final Config config;
    private AsyncIdleConnectionMonitorThread syncMonitor;
    private PoolingAsyncClientConnectionManager manager;
    private boolean hookset;

    public ApacheAsyncConfig(Config config) {
        this.config = config;
        try {
            manager = createConnectionManager();
            manager.setMaxTotal(config.getMaxConnections());
            manager.setDefaultMaxPerRoute(config.getMaxPerRoutes());

            HttpAsyncClientBuilder ab = HttpAsyncClientBuilder.create()
                    .setDefaultRequestConfig(RequestOptions.toRequestConfig(config))
                    .setConnectionManager(manager)
                    .setDefaultCredentialsProvider(toApacheCreds(config.getProxy()))
                    .useSystemProperties();

            setOptions(ab);

            CloseableHttpAsyncClient build = ab.build();
            build.start();
            syncMonitor = new AsyncIdleConnectionMonitorThread(manager);
            syncMonitor.tryStart();
            client = build;
            if (config.shouldAddShutdownHook()) {
                registerShutdownHook();
            }
        } catch (Exception e) {
            throw new UnirestConfigException(e);
        }
    }

    public ApacheAsyncConfig(CloseableHttpAsyncClient client, Config config) {
        this.client = client;
        this.config = config;
    }

    private PoolingAsyncClientConnectionManager createConnectionManager() throws Exception {
        return new PoolingAsyncClientConnectionManager(createDefault(),
                PoolConcurrencyPolicy.STRICT,
                PoolReusePolicy.LIFO,
                TimeValue.of(config.getTTL(), TimeUnit.MILLISECONDS),
                null,
                null
        );
    }

    private Registry<TlsStrategy> createDefault() {
        return RegistryBuilder.<TlsStrategy>create()
                .register("https", DefaultClientTlsStrategy.getDefault())
                .build();
    }

    private void setOptions(HttpAsyncClientBuilder ab) {
        if (!config.isVerifySsl()) {
            // disableSsl(ab);
        }
        if (config.useSystemProperties()) {
            ab.useSystemProperties();
        }
        if (!config.getFollowRedirects()) {
            ab.setRedirectStrategy(new ApacheNoRedirectStrategy());
        }
        if (!config.getEnabledCookieManagement()) {
            ab.disableCookieManagement();
        }
    }


    public void registerShutdownHook() {
        if (!hookset) {
            hookset = true;
            Runtime.getRuntime().addShutdownHook(new Thread(this::close, "Unirest Apache Async Client Shutdown Hook"));
        }
    }

    public boolean isRunning() {
        return Util.tryCast(client, CloseableHttpAsyncClient.class)
                .map(c -> c.getStatus() == IOReactorStatus.ACTIVE)
                .orElse(true);
    }

    public HttpAsyncClient getClient() {
        return client;
    }

    public Stream<Exception> close() {
        return Util.collectExceptions(Util.tryCast(client, CloseableHttpAsyncClient.class)
                        .filter(c -> c.getStatus() == IOReactorStatus.ACTIVE)
                        .map(c -> Util.tryDo(c, d -> d.close()))
                        .filter(c -> c.isPresent())
                        .map(c -> c.get()),
                Util.tryDo(manager, m -> m.close()),
                Util.tryDo(syncMonitor, m -> m.interrupt()));
    }
}
