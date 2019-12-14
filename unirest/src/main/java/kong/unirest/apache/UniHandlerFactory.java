package kong.unirest.apache;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.core5.http.protocol.HttpContext;

public class UniHandlerFactory implements HandlerFactory<AsyncPushConsumer> {
    @Override
    public AsyncPushConsumer create(HttpRequest httpRequest, HttpContext httpContext) throws HttpException {
        return null;
    }
}
