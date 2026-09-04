package com.recovery.server;

import com.recovery.model.Transaction;
import com.recovery.service.DataStore;
import com.recovery.service.RecoveryEngine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;

/**
 * ApiServer
 * ----------
 * A tiny web server built using nothing but the JDK's own
 * com.sun.net.httpserver.HttpServer class. I deliberately did NOT use
 * Spring Boot, because Spring pulls in Maven/Gradle, a dozen jar
 * dependencies, and a lot of "magic" annotations that make the code
 * harder to explain in a 5 minute video. This way, the whole backend
 * is a few plain .java files that anyone can read top to bottom.
 *
 * It does two jobs:
 *  1. Serves the static frontend files (index.html, style.css, app.js)
 *     from the /webapp folder.
 *  2. Serves a JSON API at /api/transactions and /api/retry that the
 *     frontend calls with plain fetch().
 */
public class ApiServer {

    private final DataStore dataStore;
    private final RecoveryEngine engine;
    private final String webappFolder;

    public ApiServer(DataStore dataStore, RecoveryEngine engine, String webappFolder) {
        this.dataStore = dataStore;
        this.engine = engine;
        this.webappFolder = webappFolder;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API endpoints
        server.createContext("/api/transactions", new TransactionsHandler());
        server.createContext("/api/retry", new RetryHandler());

        // Everything else -> serve static files from /webapp
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor is fine for a demo app
        server.start();
        System.out.println("AI Revenue Recovery server running at http://localhost:" + port);
    }

    /**
     * GET /api/transactions
     * Scores every transaction and returns them as a JSON array.
     */
    class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Transaction> transactions = dataStore.getAll();
            engine.scoreAll(transactions);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < transactions.size(); i++) {
                json.append(transactions.get(i).toJson());
                if (i < transactions.size() - 1) json.append(",");
            }
            json.append("]");

            sendJson(exchange, json.toString());
        }
    }

    /**
     * POST /api/retry
     * Runs the retry simulation across all transactions and returns the
     * updated list, now including whether each retry "succeeded".
     */
    class RetryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Transaction> transactions = dataStore.getAll();
            engine.scoreAll(transactions);
            engine.simulateRetries(transactions);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < transactions.size(); i++) {
                json.append(transactions.get(i).toJson());
                if (i < transactions.size() - 1) json.append(",");
            }
            json.append("]");

            sendJson(exchange, json.toString());
        }
    }

    /**
     * Serves static files (html/css/js) straight off disk from the
     * webapp folder. If the path is "/" we serve index.html.
     */
    class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            File file = new File(webappFolder, path);

            if (!file.exists() || file.isDirectory()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String contentType = guessContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                fis.transferTo(os);
            }
        }
    }

    private String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // Allow the frontend to call this from any origin during local testing
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
