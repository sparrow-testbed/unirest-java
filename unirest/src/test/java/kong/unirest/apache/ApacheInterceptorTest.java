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

import BehaviorTests.BddTest;
import BehaviorTests.MockServer;
import BehaviorTests.RequestCapture;
import kong.unirest.Unirest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequestInterceptor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApacheInterceptorTest extends BddTest {


    @Test
    void totalAsyncFailure() throws Exception {
        Unirest.config().addInterceptor((r, c) -> {
            throw new IOException("Something horrible happened");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> Unirest.get(MockServer.GET).asStringAsync().get());
        assertEquals("java.io.IOException: " + "Something horrible happened", ex.getMessage());
    }

    @Test
    void canAddApacheInterceptor() {
        Unirest.config().addInterceptor(new TestInterceptor());

        Unirest.get(MockServer.GET)
                .asObject(RequestCapture.class)
                .getBody()
                .assertHeader("x-custom", "foo");
    }

    @Test
    void canAddApacheInterceptorToAsync() throws ExecutionException, InterruptedException {
        Unirest.config().addInterceptor(new TestInterceptor());

        Unirest.get(MockServer.GET)
                .asObjectAsync(RequestCapture.class)
                .get()
                .getBody()
                .assertHeader("x-custom", "foo");
    }

    private class TestInterceptor implements HttpRequestInterceptor {
        @Override
        public void process(org.apache.http.HttpRequest httpRequest, org.apache.http.protocol.HttpContext httpContext) throws HttpException, IOException {
            httpRequest.addHeader("x-custom", "foo");
        }
    }
}
