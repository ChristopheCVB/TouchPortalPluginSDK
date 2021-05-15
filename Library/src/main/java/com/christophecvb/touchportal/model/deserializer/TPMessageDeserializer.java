/*
 * Touch Portal Plugin SDK
 *
 * Copyright 2020 Christophe Carvalho Vilas-Boas
 * christophe.carvalhovilasboas@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
