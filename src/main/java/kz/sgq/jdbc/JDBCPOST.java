package kz.sgq.jdbc;

import com.google.gson.Gson;
import kz.sgq.FS_RC4;
import kz.sgq.KeyGen;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import spark.Request;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.sql.SQLException;
import java.util.HashMap;

public class JDBCPOST {
    private final int LENGTH_AVATAR = 4;
    private final int LENGTH_TOKEN = 40;
    private final int LENGTH_NICK = 4;
    private final int LENGTH_LOGIN = 4;
    private final int LENGTH_PASSWORD = 4;
    private final int LENGTH_IDUSER = 1;
    private final int LENGTH_CONTENT = 1;

    public final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private final String key = "key=AAAAkza4aqc:APA91bGTvj2rKseIgLrLAcfo1_PmHj3Hk-ZwTM2FCh5qy1ROiP3Uu14efNgUf-Zhr1eOTex2poMNWdxmcWo8RT9be6ermMHDZNmRbkqNF_359wTrq7ovrk4MueMeHYoe-Qi8ZUqqLz1k";
    private final String URL_FCM = "https://fcm.googleapis.com/fcm/send";

    private URI dbUri = new URI(System.getenv("JAWSDB_URL"));
    private final String url = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath();
    private final String login = dbUri.getUserInfo().split(":")[0];
    private final String password = dbUri.getUserInfo().split(":")[1];
    private Connection connection;
    private Statement statement;

    public JDBCPOST() throws URISyntaxException {
        try {
            connection = DriverManager.getConnection(url, login, password);
            statement = connection.createStatement();
        } catch (SQLException e) {
            System.out.println("Error SQL Connecting");
        }
    }

    public String createUser(Request request) {
        String reply = null;
        boolean check = false;
        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM users WHERE users.login='" +
                    request.queryParams("login") + "'");
            while (resultSet.next()) {
                check = true;
            }
            if (!check) {
                if (request.queryParams("avatar").length() >= LENGTH_AVATAR &&
                        request.queryParams("nick").length() >= LENGTH_NICK &&
                        request.queryParams("login").length() >= LENGTH_LOGIN &&
                        request.queryParams("token").length() >= LENGTH_TOKEN &&
                        request.queryParams("password").length() >= LENGTH_PASSWORD) {
                    statement.execute("INSERT INTO users (avatar,nick,login,password,token) VALUES ('" +
                            request.queryParams("avatar") + "', '" +
                            request.queryParams("nick") + "', '" +
                            request.queryParams("login") + "', '" +
                            request.queryParams("password") + "', '" +
                            request.queryParams("token") + "')");
                    resultSet = statement.executeQuery("SELECT * FROM users WHERE users.login='" +
                            request.queryParams("login") + "' AND users.password='" +
                            request.queryParams("password") + "'");
                    while (resultSet.next()) {
                        HashMap<String, String> replyMap = new HashMap<>();
                        replyMap.put("idusers", resultSet.getString("idusers"));
                        replyMap.put("avatar", resultSet.getString("avatar"));
                        replyMap.put("nick", resultSet.getString("nick"));
                        replyMap.put("login", resultSet.getString("login"));
                        replyMap.put("password", resultSet.getString("password"));
                        replyMap.put("token", resultSet.getString("token"));
                        reply = new Gson().toJson(replyMap);
                    }
                }
            }
        } catch (Exception e) {
            reply = null;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return reply;
    }

    public String createFriends(Request request) {
        String reply = null;
        boolean check = false;
        try {
            if (Integer.parseInt(request.queryParams("iduser_1")) >= LENGTH_IDUSER &&
                    Integer.parseInt(request.queryParams("iduser_2")) >= LENGTH_IDUSER) {
                ResultSet resultSet = statement.executeQuery("SELECT * FROM friends WHERE friends.iduser_1=" +
                        request.queryParams("iduser_1") + " AND friends.iduser_2=" +
                        request.queryParams("iduser_2"));
                while (resultSet.next()) {
                    check = true;
                }
                if (!check) {
                    resultSet = statement.executeQuery("SELECT * FROM users WHERE users.idusers=" +
                            request.queryParams("iduser_2"));
                    while (resultSet.next()) {
                        statement.execute("INSERT INTO friends (iduser_1,iduser_2) VALUES (" +
                                request.queryParams("iduser_1") + ", " +
                                request.queryParams("iduser_2") + ")");
                        resultSet = statement.executeQuery("SELECT * FROM friends WHERE friends.iduser_1=" +
                                request.queryParams("iduser_1") + " AND friends.iduser_2=" +
                                request.queryParams("iduser_2"));
                        while (resultSet.next()) {
                            HashMap<String, String> replyMap = new HashMap<>();
                            replyMap.put("iduser_1", resultSet.getString("iduser_1"));
                            replyMap.put("iduser_2", resultSet.getString("iduser_2"));
                            replyMap.put("idfriends", resultSet.getString("idfriends"));
                            reply = new Gson().toJson(replyMap);
                        }
                    }
                }
            }
        } catch (Exception e) {
            reply = null;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return reply;
    }

    public String createMS(Request request) {
        String reply = null;
        boolean check = true;
        String token = null;
        String nick = null;
        if (request.queryParams("iduser_1").length() >= LENGTH_IDUSER &&
                request.queryParams("iduser_2").length() >= LENGTH_IDUSER &&
                request.queryParams("content").length() >= LENGTH_CONTENT) {
            try {
                ResultSet resultSet = statement.executeQuery("SELECT * FROM chats WHERE (chats.iduser_1=" +
                        request.queryParams("iduser_1") + " AND chats.iduser_2=" +
                        request.queryParams("iduser_2") + ") OR (chats.iduser_1=" +
                        request.queryParams("iduser_2") + " AND chats.iduser_2=" +
                        request.queryParams("iduser_1") + ")");
                while (resultSet.next())
                    check = false;
                if (check) {
                    String key = new KeyGen().generate(20);
                    statement.execute("INSERT INTO chats (iduser_1, iduser_2, chats.key) VALUES (" +
                            request.queryParams("iduser_1") + ", " +
                            request.queryParams("iduser_2") + ", '" +
                            key + "')");

                    resultSet = statement.executeQuery("SELECT * FROM chats WHERE (chats.iduser_1=" +
                            request.queryParams("iduser_1") + " AND chats.iduser_2=" +
                            request.queryParams("iduser_2") + ") OR (chats.iduser_1=" +
                            request.queryParams("iduser_2") + " AND chats.iduser_2=" +
                            request.queryParams("iduser_1") + ")");
                    while (resultSet.next()) {
                        HashMap<String, String> replyMap = new HashMap<>();
                        String content = new FS_RC4(key, request.queryParams("content")).start();
                        String idchats = resultSet.getString("idchats");
                        statement.execute("INSERT INTO messages (idchats,iduser,content) VALUES (" +
                                idchats + ", " +
                                request.queryParams("iduser_2") + ", '" +
                                content + "')");
                        replyMap.put("key", key);
                        replyMap.put("idchats", idchats);
                        replyMap.put("iduser", request.queryParams("iduser_2"));
                        replyMap.put("content", content);
                        resultSet = statement.executeQuery("SELECT * FROM messages WHERE messages.iduser=" +
                                request.queryParams("iduser_2") + " ORDER BY messages.idmessages DESC LIMIT 1");
                        while (resultSet.next()) {
                            replyMap.put("idmessages", resultSet.getString("idmessages"));
                            reply = new Gson().toJson(replyMap);
                        }

                        resultSet = statement.executeQuery("SELECT * FROM users WHERE users.idusers=" +
                                request.queryParams("iduser_2"));
                        while (resultSet.next()) {
                            token = resultSet.getString("token");
                        }

                        resultSet = statement.executeQuery("SELECT * FROM users WHERE users.idusers=" +
                                request.queryParams("iduser_1"));
                        while (resultSet.next()) {
                            nick = resultSet.getString("nick");
                        }
                        if (nick != null && token != null)
                            postFCM(nick, content, token);
                    }
                } else {
                    resultSet = statement.executeQuery("SELECT * FROM chats WHERE (chats.iduser_1=" +
                            request.queryParams("iduser_1") + " AND chats.iduser_2=" +
                            request.queryParams("iduser_2") + ") OR (chats.iduser_1=" +
                            request.queryParams("iduser_2") + " AND chats.iduser_2=" +
                            request.queryParams("iduser_1") + ")");
                    while (resultSet.next()) {
                        HashMap<String, String> replyMap = new HashMap<>();
                        String idchats = resultSet.getString("idchats");
                        statement.execute("INSERT INTO messages (idchats,iduser,content) VALUES (" +
                                idchats + ", " +
                                request.queryParams("iduser_2") + ", '" +
                                request.queryParams("content") + "')");
                        replyMap.put("idchats", idchats);
                        replyMap.put("iduser", request.queryParams("iduser_2"));
                        replyMap.put("content", request.queryParams("content"));
                        resultSet = statement.executeQuery("SELECT * FROM messages WHERE messages.iduser=" +
                                request.queryParams("iduser_2") + " ORDER BY messages.idmessages DESC LIMIT 1");
                        while (resultSet.next()) {
                            replyMap.put("idmessages", resultSet.getString("idmessages"));
                            reply = new Gson().toJson(replyMap);
                        }

                        resultSet = statement.executeQuery("SELECT * FROM users WHERE users.idusers=" +
                                request.queryParams("iduser_2"));
                        while (resultSet.next()) {
                            token = resultSet.getString("token");
                        }

                        resultSet = statement.executeQuery("SELECT * FROM users WHERE users.idusers=" +
                                request.queryParams("iduser_1"));
                        while (resultSet.next()) {
                            nick = resultSet.getString("nick");
                        }
                        if (nick != null && token != null)
                            postFCM(nick, request.queryParams("content"), token);
                    }
                }
            } catch (Exception e) {
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return reply;
        } else {
            return reply;
        }
    }

    private void postFCM(String title, String body, String token) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String json = "{" +
                "  \"to\": \" " + token + "\", " +
                "  \"notification\": {" +
                "    \"title\":\"" + title + "\"," +
                "    \"body\":\"" + body + "\"" +
                "  }" +
                "}";
        RequestBody requestBody = RequestBody.create(JSON, json);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .header("Authorization", key)
                .url(url)
                .post(requestBody)
                .build();
        client.newCall(request).execute();
    }
}
