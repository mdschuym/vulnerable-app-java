package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class LoginHandler implements HttpHandler {

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    private static final Map<String, String> USERS = new HashMap<>();
    static {
        USERS.put("admin", "0192023a7bbd73250516f069df18b500");
        USERS.put("alice", "5f4dcc3b5aa765d61d8327deb882cf99");
        USERS.put("bob", "0d107d09f5bbe40cade3de5c71e9e9b7");
    }

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            sendLoginPage(exchange, "");
        } else if (method.equalsIgnoreCase("POST")) {
            handleLogin(exchange);
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes());
        Map<String, String> params = parseFormData(body);

        String username = params.getOrDefault("username", "");
        String password = params.getOrDefault("password", "");

        // SQL Injection vulnerability - raw string concatenation
        String query = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";

        // Weak MD5 hashing
        String hashedInput = md5(password);

        // No brute force / lockout protection
        if (ADMIN_USER.equals(username) && ADMIN_PASS.equals(password)) {
            sendDashboard(exchange, username);
        } else if (USERS.containsKey(username) && USERS.get(username).equals(hashedInput)) {
            sendDashboard(exchange, username);
        } else {
            // XSS: username echoed back unsanitized
            sendLoginPage(exchange, "Invalid login for user: " + username);
        }
    }

    private void sendLoginPage(HttpExchange exchange, String message) throws IOException {
        String errorBlock = message.isEmpty() ? "" : "<div class=\"error\">" + message + "</div>";
        String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>Login</title><style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; }" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f172a; display: flex; align-items: center; justify-content: center; min-height: 100vh; }" +
            ".card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 40px; width: 100%; max-width: 400px; box-shadow: 0 25px 50px rgba(0,0,0,0.5); }" +
            ".logo { text-align: center; margin-bottom: 32px; }" +
            ".logo h1 { color: #f8fafc; font-size: 24px; font-weight: 700; }" +
            ".logo p { color: #64748b; font-size: 14px; margin-top: 4px; }" +
            ".form-group { margin-bottom: 20px; }" +
            "label { display: block; color: #94a3b8; font-size: 13px; font-weight: 500; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.05em; }" +
            "input { width: 100%; padding: 12px 16px; background: #0f172a; border: 1px solid #334155; border-radius: 8px; color: #f8fafc; font-size: 15px; outline: none; }" +
            "input:focus { border-color: #6366f1; }" +
            "button { width: 100%; padding: 12px; background: #6366f1; color: white; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; margin-top: 8px; }" +
            "button:hover { background: #4f46e5; }" +
            ".error { background: #450a0a; border: 1px solid #991b1b; color: #fca5a5; padding: 12px 16px; border-radius: 8px; font-size: 14px; margin-bottom: 20px; }" +
            ".hint { text-align: center; margin-top: 24px; color: #475569; font-size: 12px; }" +
            "</style></head><body>" +
            "<div class=\"card\"><div class=\"logo\"><h1>SecureApp</h1><p>Definitely not vulnerable</p></div>" +
            errorBlock +
            "<form method=\"POST\" action=\"/login\">" +
            "<div class=\"form-group\"><label>Username</label><input type=\"text\" name=\"username\" placeholder=\"Enter username\" autofocus /></div>" +
            "<div class=\"form-group\"><label>Password</label><input type=\"password\" name=\"password\" placeholder=\"Enter password\" /></div>" +
            "<button type=\"submit\">Sign In</button></form>" +
            "<div class=\"hint\">Hint: try admin / admin123</div>" +
            "</div></body></html>";

        byte[] bytes = html.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendDashboard(HttpExchange exchange, String username) throws IOException {
        String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Dashboard</title><style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; }" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; }" +
            ".card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 40px; text-align: center; max-width: 400px; }" +
            "h1 { font-size: 28px; margin-bottom: 8px; }" +
            "p { color: #64748b; }" +
            ".badge { display: inline-block; background: #14532d; border: 1px solid #166534; color: #86efac; padding: 4px 12px; border-radius: 999px; font-size: 13px; margin-top: 16px; }" +
            "</style></head><body>" +
            "<div class=\"card\"><h1>Welcome, " + username + "!</h1>" +
            "<p>You are now logged in.</p>" +
            "<div class=\"badge\">Authenticated</div></div>" +
            "</body></html>";

        byte[] bytes = html.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, String> parseFormData(String data) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        for (String pair : data.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(kv[1], "UTF-8"));
            }
        }
        return map;
    }
}