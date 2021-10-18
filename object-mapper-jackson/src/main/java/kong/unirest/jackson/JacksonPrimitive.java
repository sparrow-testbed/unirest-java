package kong.unirest.jackson;

import com.fasterxml.jackson.databind.node.ValueNode;
import kong.unirest.json.EnginePrimitive;

class JacksonPrimitive extends JacksonElement<ValueNode> implements EnginePrimitive {
    public JacksonPrimitive(ValueNode element) {
        super(element);
    }

    @Override
    public boolean isBoolean() {
        return element.isBoolean();
    }

    @Override
    public boolean isNumber() {
        return element.isNumber();
    }
}
