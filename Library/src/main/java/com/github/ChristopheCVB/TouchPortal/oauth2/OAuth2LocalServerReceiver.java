/*
 *
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
 *
 */

package com.github.ChristopheCVB.TouchPortal.oauth2;

import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class OAuth2LocalServerReceiver {
    private static final String LOCALHOST = "localhost";
    private static final String CALLBACK_PATH = "/oauth2";

    private final String host;
    private final int port;
    private final String callbackPath;

    private OAuth2LocalServerReceiver(String host, int port, String callbackPath) {
        this.host = host;
        this.port = port != -1 ? port : this.findOpenPort();
        this.callbackPath = callbackPath;
    }

    public void waitForCode(URI authorizationURI, OAuth2CodeListener oAuth2CodeListener) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(authorizationURI);
                }
            }

            HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.host, this.port), 0);

            httpServer.createContext(this.callbackPath, httpExchange -> {
                String requestMethod = httpExchange.getRequestMethod();
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                if ("POST".equalsIgnoreCase(requestMethod) || "GET".equalsIgnoreCase(requestMethod)) {
                    Map<String, String> params = this.queryToMap(httpExchange.getRequestURI().getQuery());
                    String oAuth2Code = params.get("code");
                    String oAuth2Error = params.get("error");
                    oAuth2CodeListener.onOAuth2Code(oAuth2Code, oAuth2Error);

                    OutputStream outputStream = httpExchange.getResponseBody();
                    String response = "{\"success\": true}";
                    httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                    httpExchange.sendResponseHeaders(200, response.length());
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                    outputStream.close();

                    httpServer.stop(0);
                }
                else if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
                    httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                    httpExchange.sendResponseHeaders(204, -1);
                }
            });
            httpServer.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findOpenPort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            Throwable throwable = null;

            int openPort;
            try {
                socket.setReuseAddress(true);
                openPort = socket.getLocalPort();
            }
            catch (Throwable thrown) {
                throwable = thrown;
                throw thrown;
            }
            finally {
                if (socket != null) {
                    if (throwable != null) {
                        try {
                            socket.close();
                        }
                        catch (Throwable suppressedThrown) {
                            throwable.addSuppressed(suppressedThrown);
                        }
                    }
                    else {
                        socket.close();
                    }
                }
            }

            return openPort;
        }
        catch (IOException e) {
            throw new IllegalStateException("No free TCP/IP port to start embedded HTTP Server on");
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            String[] params = query.split("&");

            for (String param : params) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    result.put(pair[0], pair[1]);
                }
                else {
                    result.put(pair[0], "");
                }
            }
        }

        return result;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getCallbackPath() {
        return this.callbackPath;
    }

    public interface OAuth2CodeListener {
        void onOAuth2Code(String oAuth2Code, String error);
    }

    public static final class Builder {
        private String host = OAuth2LocalServerReceiver.LOCALHOST;
        private int port = -1;
        private String callbackPath = OAuth2LocalServerReceiver.CALLBACK_PATH;

        public Builder() {
        }

        public String getHost() {
            return this.host;
        }

        public Builder setHost(String host) {
            this.host = host;

            return this;
        }

        public int getPort() {
            return this.port;
        }

        public Builder setPort(int port) {
            this.port = port;

            return this;
        }

        public String getCallbackPath() {
            return this.callbackPath;
        }

        public Builder setCallbackPath(String callbackPath) {
            this.callbackPath = callbackPath;

            return this;
        }

        public OAuth2LocalServerReceiver build() {
            return new OAuth2LocalServerReceiver(this.host, this.port, this.callbackPath);
        }
    }
}
