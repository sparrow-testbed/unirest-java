package kong.unirest.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import kong.unirest.json.EngineArray;
import kong.unirest.json.EngineElement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class JacksonArray extends JacksonElement<ArrayNode> implements EngineArray {
    JacksonArray(ArrayNode element) {
        super(element);
    }

    @Override
    public int size() {
        return element.size();
    }

    @Override
    public EngineElement get(int index) {
        return wrap(element.get(index));
    }


    @Override
    public EngineElement remove(int index) {
        element.remove(index);
        return this;
    }

    @Override
    public EngineElement put(int index, Number number) {
        if(number instanceof Integer){
            element.insert(index, (Integer) number);
        } else if (number instanceof Double){
            element.insert(index, (Double)number);
        } else if (number instanceof BigInteger) {
            element.insert(index, (BigInteger) number);
        } else if (number instanceof Float){
            element.insert(index, (Float)number);
        } else if(number instanceof BigDecimal) {
            element.insert(index, (BigDecimal) number);
        }
        return this;
    }

    @Override
    public EngineElement put(int index, String value) {
        element.insert(index, value);
        return this;
    }

    @Override
    public EngineElement put(int index, Boolean value) {
        element.insert(index, value);
        return this;
    }

    @Override
    public void add(EngineElement obj) {
        element.add((JsonNode) obj.getEngineElement());
    }

    @Override
    public void set(int index, EngineElement o) {
        element.set(index, o.getEngineElement());
    }

    @Override
    public void add(Number number) {
        if(number instanceof Integer){
            element.add((Integer) number);
        } else if (number instanceof Double){
            element.add((Double)number);
        } else if (number instanceof BigInteger) {
            element.add((BigInteger) number);
        } else if (number instanceof Float){
            element.add((Float)number);
        } else if(number instanceof BigDecimal) {
            element.add((BigDecimal) number);
        }
    }

    @Override
    public void add(String str) {
        element.add(str);
    }

    @Override
    public void add(Boolean bool) {
        element.add(bool);
    }

    @Override
    public String join(String token) {
        return StreamSupport.stream(element.spliterator(), false)
                .map(String::valueOf)
                .collect(Collectors.joining(token));
    }
}
