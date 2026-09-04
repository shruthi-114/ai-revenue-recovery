package com.recovery;

import com.recovery.server.ApiServer;
import com.recovery.service.DataStore;
import com.recovery.service.RecoveryEngine;

/**
 * Main
 * -----
 * Entry point for the whole project. Run this file and it starts a
 * local web server on port 8080. Open http://localhost:8080 in your
 * browser to see the dashboard.
 *
 * How to run (no build tool needed, just the JDK):
 *   javac -d out $(find src -name "*.java")
 *   java -cp out com.recovery.Main
 */
public class Main {
    public static void main(String[] args) throws Exception {
        DataStore dataStore = new DataStore();
        RecoveryEngine engine = new RecoveryEngine();

        int port = 8081;
        ApiServer server = new ApiServer(dataStore, engine, "webapp");
        server.start(port);
    }
}
