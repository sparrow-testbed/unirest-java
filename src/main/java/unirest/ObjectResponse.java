/**
 * The MIT License
 *
 * Copyright for portions of OpenUnirest/uniresr-java are held by Mashape (c) 2013 as part of Kong/unirest-java.
 * All other copyright for OpenUnirest/unirest-java are held by OpenUnirest (c) 2018.
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

package unirest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;

import static unirest.Util.readString;

class ObjectResponse<T> extends BaseResponse<T> {
    private final T body;
    private final ObjectMapper om;

    ObjectResponse(ObjectMapper om, RawResponse response, Class<? extends T> to) {
        super(response);
        this.om = om;
        this.body = readBody(response)
                .map(s -> getBody(s, e -> om.readValue(e, to)))
                .orElse(null);
    }

    ObjectResponse(ObjectMapper om, RawResponse response, GenericType<? extends T> to){
        super(response);
        this.om = om;
        this.body = readBody(response)
                .map(s -> getBody(s, e -> om.readValue(e, to)))
                .orElse(null);
    }

    private Optional<String> readBody(RawResponse response) {
        if(!response.hasContent()){
            return Optional.empty();
        }
        return Optional.of(readString(response));
    }

    private T getBody(String b, Function<String, T> func){
        try {
            return func.apply(b);
        } catch (RuntimeException e) {
            setParsingException(b, e);
            return null;
        }
    }

    @Override
    public InputStream getRawBody() {
        if(body == null){
            return new ByteArrayInputStream(new byte[0]);
        }
        return new ByteArrayInputStream(om.writeValue(body).getBytes());
    }

    @Override
    public T getBody() {
        return body;
    }
}
