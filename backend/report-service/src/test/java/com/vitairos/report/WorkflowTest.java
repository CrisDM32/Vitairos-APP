package com.vitairos.report;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowTest {
    @Test void weatherIsCapturedOnceAtCreation() throws Exception {
        var provider = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        provider.createContext("/v1/forecast", exchange -> {
            try (exchange) {
                int count = requests.incrementAndGet();
                if (count > 1) {
                    exchange.sendResponseHeaders(503,-1);
                    return;
                }
                byte[] body = """
                    {"latitude":-33.4,"current":{"time":1795626000,"interval":900,"temperature_2m":30.4,
                    "relative_humidity_2m":17,"wind_speed_10m":5.2,"weather_code":0}}
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type","application/json");
                exchange.sendResponseHeaders(200,body.length);
                exchange.getResponseBody().write(body);
            }
        });
        provider.start();
        try (var identity = start(com.vitairos.identity.Application.class,"weatherAccounts");
             var reports = start(Application.class,"weatherReports","--identity.url="+base(identity).replace("/api/v1",""),
                     "--weather.url=http://127.0.0.1:"+provider.getAddress().getPort())) {
            String operator = token(base(identity),"operador");
            String url = base(reports)+"/reports/";
            String type = "application/json";
            String form = """
                {"type":"Reporte de turno v1","process":"Molienda","area":"Planta","shift":"Día administrativo",
                "latitude":-33.45,"longitude":-70.66}
                """;
            var saved = call("PUT",url+"con-clima",operator,type,form);
            assertEquals(200,saved.statusCode(),saved.body());
            var weather = json.readTree(saved.body()).get("weather");
            assertEquals(30.4,weather.get("temperatureC").asDouble());
            assertEquals(java.time.Instant.ofEpochSecond(1795626000).toString(),weather.get("observedAt").asString());
            var edited = call("PUT",url+"con-clima",operator,type,form.replace("Molienda","Chancado"));
            assertEquals(weather,json.readTree(edited.body()).get("weather"));
            assertEquals(weather,json.readTree(call("GET",url+"con-clima",operator,type,"").body()).get("weather"));
            assertEquals(1,requests.get());
            var unavailable = call("PUT",url+"sin-clima",operator,type,form);
            assertEquals(200,unavailable.statusCode(),unavailable.body());
            assertTrue(json.readTree(unavailable.body()).get("weather").isNull());
            assertEquals(2,requests.get());
            String current = base(reports)+"/weather/current?latitude=-33.45&longitude=-70.66";
            assertEquals(401,call("GET",current,null,type,"").statusCode());
            assertEquals(400,call("GET",current.replace("-33.45","91"),operator,type,"").statusCode());
            assertEquals(503,call("GET",current,operator,type,"").statusCode());
        } finally {
            provider.stop(0);
        }
    }
    final HttpClient client = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();

    ServletWebServerApplicationContext start(Class<?> app, String db, String... extra) {
        var args = new java.util.ArrayList<>(java.util.List.of("--server.port=0", "--spring.datasource.url=jdbc:h2:mem:"+db,
                "--account.operator-password=test-operador", "--account.supervisor-password=test-supervisor"));
        args.addAll(java.util.List.of(extra));
        return (ServletWebServerApplicationContext)new SpringApplicationBuilder(app).run(args.toArray(String[]::new));
    }
    String base(ServletWebServerApplicationContext app) {
        return "http://127.0.0.1:"+app.getWebServer().getPort()+"/api/v1";
    }
    HttpResponse<String> call(String method, String url, String token, String type, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).header("Content-Type",type);
        if (token != null) request.header("Authorization","Bearer "+token);
        return client.send(request.method(method,HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    String token(String identity, String role) throws Exception {
        var login = call("POST",identity+"/sessions",null,"application/json",
                "{\"email\":\""+role+"@vitairos.demo\",\"password\":\"test-"+role+"\"}");
        assertEquals(201,login.statusCode(),login.body());
        return json.readTree(login.body()).get("accessToken").asString();
    }

    @Test void submitReviewCorrectAndRevoke() throws Exception {
        try (var identity = start(com.vitairos.identity.Application.class,"accounts");
             var reports = start(Application.class,"reports","--identity.url="+base(identity).replace("/api/v1",""))) {
            String operator = token(base(identity),"operador");
            String supervisor = token(base(identity),"supervisor");
            String url = base(reports)+"/reports/RPT-2026-0001";
            String type = "application/json";
            String draft = """
                {"type":"Reporte de turno v1","process":"Molienda","area":"Planta","shift":"Día administrativo"}
                """;
            assertEquals(401,call("GET",url,null,type,"").statusCode());
            assertEquals(400,call("PUT",url,operator,type,"{}").statusCode());
            var saved = call("PUT",url,operator,type,draft);
            assertEquals(200,saved.statusCode(),saved.body());
            assertEquals(409,call("PUT",url+"/submission",operator,type,"").statusCode());
            String file = "--part\r\nContent-Disposition: form-data; name=\"file\"; filename=\"turno.pdf\"\r\nContent-Type: application/pdf\r\n\r\n%PDF-1.7\r\n--part--\r\n";
            var upload = call("PUT",url+"/attachments/A1",operator,"multipart/form-data; boundary=part",file);
            assertEquals(200,upload.statusCode(),upload.body());
            assertEquals("%PDF-1.7",call("GET",url+"/attachments/A1",operator,type,"").body());
            var submitted = call("PUT",url+"/submission",operator,type,"");
            assertEquals(200,submitted.statusCode(),submitted.body());
            assertEquals(submitted.body(),call("PUT",url+"/submission",operator,type,"").body());
            String note = "{\"note\":\"Revise el turno\"}";
            assertEquals(403,call("PUT",url+"/correction",operator,type,note).statusCode());
            assertEquals(200,call("PUT",url+"/correction",supervisor,type,note).statusCode());
            assertEquals(200,call("PUT",url,operator,type,draft.replace("Molienda","Chancado")).statusCode());
            var resent = call("PUT",url+"/submission",operator,type,"");
            assertEquals(2,json.readTree(resent.body()).at("/report/attempts").asInt());
            assertEquals(204,call("DELETE",base(identity)+"/sessions/current",operator,type,"").statusCode());
            assertEquals(401,call("GET",url,operator,type,"").statusCode());
        }
    }
}
