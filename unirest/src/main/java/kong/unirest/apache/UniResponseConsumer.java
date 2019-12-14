package kong.unirest.apache;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class UniResponseConsumer implements AsyncResponseConsumer {
    @Override
    public void consumeResponse(HttpResponse httpResponse, EntityDetails entityDetails, HttpContext httpContext, FutureCallback futureCallback) throws HttpException, IOException {

    }

    @Override
    public void informationResponse(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {

    }

    @Override
    public void failed(Exception e) {

    }

    @Override
    public void updateCapacity(CapacityChannel capacityChannel) throws IOException {

    }

    @Override
    public void consume(ByteBuffer byteBuffer) throws IOException {

    }

    @Override
    public void streamEnd(List<? extends Header> list) throws HttpException, IOException {

    }

    @Override
    public void releaseResources() {

    }
}
