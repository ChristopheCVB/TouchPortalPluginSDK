package com.christophecvb.touchportal.model.deserializer;

import com.christophecvb.touchportal.helpers.ReceivedMessageHelper;
import com.christophecvb.touchportal.model.TPMessage;
import com.christophecvb.touchportal.model.TPSettingsMessage;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class TPMessageDeserializer implements JsonDeserializer<TPMessage> {
    private final Map<String, Class<? extends TPMessage>> registry;

    public TPMessageDeserializer() {
        this.registry = new HashMap<>();
    }

    public void registerTPMessageType(String type, Class<? extends TPMessage> tpMessageType) {
        this.registry.put(type, tpMessageType);
    }

    @Override
    public TPMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        TPMessage tpMessage = new TPMessage();

        JsonObject jsonTPMessage = json.getAsJsonObject();
        JsonElement tpMessageTypeElement = jsonTPMessage.get("type");
        String type = tpMessageTypeElement.getAsString();
        Class<? extends TPMessage> eventClass = this.registry.get(type);
        if (eventClass != null) {
            tpMessage = context.deserialize(json, eventClass);

            if (tpMessage instanceof TPSettingsMessage) {
                TPSettingsMessage tpSettingsMessage = (TPSettingsMessage) tpMessage;
                tpSettingsMessage.settings = ReceivedMessageHelper.getSettings(jsonTPMessage.has(ReceivedMessageHelper.SETTINGS) ? jsonTPMessage.get(ReceivedMessageHelper.SETTINGS) : jsonTPMessage.get(ReceivedMessageHelper.VALUES));
            }
        }
        else {
            tpMessage.type = type;
        }

        return tpMessage;
    }
}
