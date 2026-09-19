package com.voicestock.database;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("postgres")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.config.import=")
class MvpWorkflowIT {
    @DynamicPropertySource static void config(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestDatabase.required("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> TestDatabase.required("TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> TestDatabase.required("TEST_DB_PASSWORD"));
        registry.add("app.jwt.secret", () -> TestDatabase.JWT_SECRET);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder jwtEncoder;
    final List<UUID> createdUsers=new ArrayList<>();
    final HttpClient client=HttpClient.newHttpClient();
    static final String PASSWORD="Test-only password 2026!";
    record Session(String token, String id, String email) {}

    HttpResponse<String> request(String method,String path,JSONObject body,String token) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api"+path)).timeout(Duration.ofSeconds(15));
        if(token!=null) builder.header("Authorization","Bearer "+token);
        if(body!=null) builder.header("Content-Type","application/json");
        return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
    }
    JSONObject json(HttpResponse<String> response,int status) throws Exception {
        assertThat(response.statusCode()).withFailMessage("Expected HTTP %s, got %s",status,response.statusCode()).isEqualTo(status);
        return new JSONObject(response.body());
    }
    Session register() throws Exception {
        String email=UUID.randomUUID()+"@example.invalid";
        var data=json(request("POST","/auth/register",new JSONObject().put("name","Test owner").put("email",email).put("password",PASSWORD)
            .put("businessName","Test shop").put("preferredLanguage","en"),null),201);
        String id=data.getJSONObject("user").getString("id"); createdUsers.add(UUID.fromString(id));
        return new Session(data.getString("token"),id,email);
    }
    JSONObject product(String name,String unit,int stock,int minimum) throws Exception {
        return new JSONObject().put("name",name).put("category","Grocery").put("unit",unit).put("currentStock",stock).put("minimumStock",minimum).put("price",1200);
    }
    JSONObject stock(String id,int quantity,String unit) throws Exception { return new JSONObject().put("productId",id).put("quantity",quantity).put("unit",unit); }
    @AfterEach void cleanup() {
        for(UUID id:createdUsers) {
            jdbc.update("DELETE FROM business_actions WHERE user_id=?",id);
            jdbc.update("DELETE FROM inventory_transactions WHERE user_id=?",id);
            jdbc.update("DELETE FROM products WHERE user_id=?",id);
            jdbc.update("DELETE FROM users WHERE id=?",id);
        }
    }

    @Test void requestedWorkflowAndUserIsolation() throws Exception {
        Session owner=register(); String token=owner.token();
        String hash=jdbc.queryForObject("SELECT password FROM users WHERE id=?",String.class,UUID.fromString(owner.id()));
        assertThat(hash).isNotEqualTo(PASSWORD); assertThat(passwords.matches(PASSWORD,hash)).isTrue();
        var login=json(request("POST","/auth/login",new JSONObject().put("email",owner.email().toUpperCase()).put("password",PASSWORD),null),200);
        assertThat(login.getJSONObject("user").has("passwordHash")).isFalse();
        assertThat(request("GET","/products",null,null).statusCode()).isEqualTo(401);
        assertThat(request("POST","/auth/login",new JSONObject().put("email",owner.email()).put("password","incorrect"),null).statusCode()).isEqualTo(401);
        String rice=json(request("POST","/products",product("Rice","BAGS",50,10),token),201).getString("id");
        assertThat(json(request("POST","/inventory/add",stock(rice,20,"BAGS").put("source","VOICE"),token),200).getDouble("currentStock")).isEqualTo(70);
        assertThat(json(request("POST","/inventory/remove",stock(rice,5,"BAGS"),token),200).getDouble("currentStock")).isEqualTo(65);
        assertThat(request("POST","/inventory/remove",stock(rice,100,"BAGS"),token).statusCode()).isEqualTo(400);
        assertThat(request("POST","/inventory/add",stock(rice,1,"KG"),token).statusCode()).isEqualTo(400);
        assertThat(request("POST","/inventory/add",stock(rice,0,"BAGS"),token).statusCode()).isEqualTo(400);
        assertThat(json(request("GET","/products/"+rice,null,token),200).getDouble("currentStock")).isEqualTo(65);
        json(request("POST","/products",product("Sugar","KG",4,5),token),201);
        json(request("POST","/products",product("Biscuits","BOXES",0,5),token),201);
        var dashboard=json(request("GET","/dashboard?timeZone=Asia/Calcutta",null,token),200);
        assertThat(dashboard.getInt("totalProducts")).isEqualTo(3);
        assertThat(dashboard.getInt("lowStock")).isEqualTo(1);
        assertThat(dashboard.getInt("outOfStock")).isEqualTo(1);
        assertThat(dashboard.getInt("todaysTransactions")).isEqualTo(4);
        var alerts=json(request("GET","/inventory/low-stock",null,token),200).getJSONArray("items");
        assertThat(alerts.length()).isEqualTo(2);
        for(int i=0;i<alerts.length();i++) {
            var p=alerts.getJSONObject(i);
            assertThat(p.getDouble("reorderQuantity")).isEqualTo(p.getString("name").equals("Sugar")?6:10);
        }
        var history=json(request("GET","/transactions",null,token),200).getJSONArray("items");
        assertThat(history.length()).isEqualTo(4);
        for(int i=0;i<history.length();i++) assertThat(history.getJSONObject(i).getString("source")).isEqualTo("MANUAL");
        assertThat(json(request("GET","/products?search=grocer",null,token),200).getLong("totalElements")).isEqualTo(3);
        Session other=register();
        assertThat(json(request("GET","/products",null,other.token()),200).getInt("totalElements")).isZero();
        assertThat(request("GET","/products/"+rice,null,other.token()).statusCode()).isEqualTo(404);
        assertThat(request("PUT","/products/"+rice,product("Stolen","BAGS",0,10),other.token()).statusCode()).isEqualTo(404);
        assertThat(request("DELETE","/products/"+rice,null,other.token()).statusCode()).isEqualTo(404);
        assertThat(request("POST","/inventory/add",stock(rice,20,"BAGS"),other.token()).statusCode()).isEqualTo(404);
        assertThat(json(request("GET","/transactions?productId="+rice,null,other.token()),200).getInt("totalElements")).isZero();
        assertThat(request("PUT","/products/"+rice,product("Rice","KG",65,10),token).statusCode()).isEqualTo(400);
        var edited=json(request("PUT","/products/"+rice,product("Rice premium","BAGS",999,10),token),200);
        assertThat(edited.getDouble("currentStock")).isEqualTo(65);
        assertThat(edited.getString("name")).isEqualTo("Rice premium");
        json(request("PUT","/users/profile",new JSONObject().put("name","Updated owner").put("businessName","Updated shop").put("preferredLanguage","te"),token),200);
        assertThat(json(request("GET","/users/profile",null,token),200).getString("preferredLanguage")).isEqualTo("te");
        assertThat(request("DELETE","/products/"+rice,null,token).statusCode()).isEqualTo(204);
        assertThat(json(request("GET","/products/"+rice,null,token),200).getBoolean("archived")).isTrue();
        assertThat(json(request("GET","/transactions",null,token),200).getInt("totalElements")).isEqualTo(4);
        assertThat(request("POST","/inventory/remove",stock(rice,1,"BAGS"),token).statusCode()).isEqualTo(404);
    }

    @Test void concurrentRemovalsCannotOversellAndDoNotWriteRejectedTransactions() throws Exception {
        var owner=register();
        String id=json(request("POST","/products",product("Concurrent rice","BAGS",10,3),owner.token()),201).getString("id");
        java.util.function.Supplier<Integer> remove=() -> { try { return request("POST","/inventory/remove",stock(id,7,"BAGS"),owner.token()).statusCode(); } catch(Exception e) { throw new RuntimeException(e); } };
        var first=CompletableFuture.supplyAsync(remove); var second=CompletableFuture.supplyAsync(remove);
        assertThat(List.of(first.join(),second.join())).containsExactlyInAnyOrder(200,400);
        var product=json(request("GET","/products/"+id,null,owner.token()),200);
        assertThat(product.getDouble("currentStock")).isEqualTo(3);
        assertThat(product.getString("status")).isEqualTo("LOW_STOCK"); // equality boundary
        assertThat(json(request("GET","/transactions",null,owner.token()),200).getInt("totalElements")).isEqualTo(2);
    }

    @Test void invalidAndExpiredTokensAndDuplicateRegistrationAreRejected() throws Exception {
        var owner=register();
        assertThat(request("GET","/products",null,"not-a-jwt").statusCode()).isEqualTo(401);
        var claims=JwtClaimsSet.builder().issuer("voicestock").subject(owner.id()).issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600)).build();
        String expired=jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),claims)).getTokenValue();
        assertThat(request("GET","/products",null,expired).statusCode()).isEqualTo(401);
        assertThat(request("POST","/auth/register",new JSONObject().put("name","Again").put("email",owner.email()).put("password",PASSWORD).put("businessName","Shop").put("preferredLanguage","en"),null).statusCode()).isEqualTo(409);
    }
    @Test void assistantPreviewConfirmationGroundingAndSafety() throws Exception {
        var owner=register(); String token=owner.token();
        String rice=json(request("POST","/products",product("Rice","BAGS",65,10),token),201).getString("id");
        json(request("POST","/products",product("Sugar","KG",4,5),token),201);
        json(request("POST","/products",product("Biscuits","BOXES",0,5),token),201);
        String[] commands={"Add 20 bags of rice","Rice 5 bags add cheyyi","Rice mein se 5 bags hatao"};
        int[] expected={85,90,85}; int before=65;
        for(int i=0;i<commands.length;i++) {
            var preview=json(request("POST","/assistant/interpret",new JSONObject().put("text",commands[i]),token),200);
            assertThat(preview.getDouble("afterStock")).isEqualTo(expected[i]);
            assertThat(json(request("GET","/products/"+rice,null,token),200).getDouble("currentStock")).isEqualTo(before);
            String id=preview.getString("confirmationId");
            assertThat(request("POST","/assistant/confirm/"+id,null,register().token()).statusCode()).isEqualTo(404);
            assertThat(json(request("POST","/assistant/confirm/"+id,null,token),200).getDouble("currentStock")).isEqualTo(expected[i]);
            assertThat(json(request("POST","/assistant/confirm/"+id,null,token),200).getDouble("currentStock")).isEqualTo(expected[i]);
            before=expected[i];
        }
        var answer=json(request("POST","/assistant/interpret",new JSONObject().put("text","How much rice do I have?"),token),200);
        assertThat(answer.getString("message")).isEqualTo("You have 85 bags of Rice.");
        for(String invalid:List.of("Remove 500 bags of rice","Add rice","Add 0 bags rice","Add 2 kg rice","Add 2 bags unknown")) {
            var reply=json(request("POST","/assistant/interpret",new JSONObject().put("text",invalid),token),200);
            assertThat(reply.isNull("confirmationId")).isTrue(); assertThat(reply.isNull("afterStock")).isTrue();
        }
        var cancel=json(request("POST","/assistant/interpret",new JSONObject().put("text","Add 100 bags of rice"),token),200);
        String cancelled=cancel.getString("confirmationId");
        assertThat(request("DELETE","/assistant/confirm/"+cancelled,null,token).statusCode()).isEqualTo(200);
        assertThat(request("POST","/assistant/confirm/"+cancelled,null,token).statusCode()).isEqualTo(404);
        assertThat(json(request("GET","/products/"+rice,null,token),200).getDouble("currentStock")).isEqualTo(85);
        var low=json(request("POST","/assistant/interpret",new JSONObject().put("text","Which products are running low?"),token),200).getJSONArray("products");
        assertThat(low.length()).isEqualTo(1);assertThat(low.getJSONObject(0).getString("name")).isEqualTo("Sugar");
        var out=json(request("POST","/assistant/interpret",new JSONObject().put("text","Which products are out of stock?"),token),200).getJSONArray("products");
        assertThat(out.length()).isEqualTo(1);assertThat(out.getJSONObject(0).getString("name")).isEqualTo("Biscuits");
        var reorder=json(request("POST","/assistant/interpret",new JSONObject().put("text","What needs to be ordered?"),token),200).getJSONArray("products");
        assertThat(reorder.length()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_transactions WHERE user_id=? AND source='VOICE'",Integer.class,UUID.fromString(owner.id()))).isEqualTo(3);
        var stale=json(request("POST","/assistant/interpret",new JSONObject().put("text","Add 1 bags rice"),token),200);
        json(request("POST","/inventory/add",stock(rice,1,"BAGS"),token),200);
        assertThat(request("POST","/assistant/confirm/"+stale.getString("confirmationId"),null,token).statusCode()).isEqualTo(409);
    }
    @Test void naturalCommandsResolveCatalogAndPreservePartialInformation() throws Exception {
        var owner=register(); String t=owner.token();
        String chips=json(request("POST","/products",product("Chips","PACKETS",20,10),t),201).getString("id");
        json(request("POST","/products",product("Potato Chips","PACKETS",20,10),t),201);
        var partial=json(request("POST","/assistant/interpret",new JSONObject().put("text","10 packets of chips"),t),200);
        assertThat(partial.getJSONObject("command").getString("product")).isEqualTo("Chips");
        assertThat(partial.getJSONObject("command").getJSONArray("missingFields").toString()).contains("intent");
        assertThat(partial.isNull("confirmationId")).isTrue();
        var chosen=partial.getJSONObject("command").put("intent","ADD_STOCK");
        var preview=json(request("POST","/assistant/preview",chosen,t),200);
        assertThat(json(request("GET","/products/"+chips,null,t),200).getDouble("currentStock")).isEqualTo(20);
        assertThat(json(request("POST","/assistant/confirm/"+preview.getString("confirmationId"),null,t),200).getDouble("currentStock")).isEqualTo(30);
        var multi=json(request("POST","/assistant/interpret",new JSONObject().put("text","Sold 2 packets potato chips"),t),200);
        assertThat(multi.getJSONObject("product").getString("name")).isEqualTo("Potato Chips");
        json(request("POST","/products",product("Basmati Rice","BAGS",20,10),t),201);
        var unique=json(request("POST","/assistant/interpret",new JSONObject().put("text","Rice entha undi?"),t),200);
        assertThat(unique.getJSONObject("product").getString("name")).isEqualTo("Basmati Rice");
        json(request("POST","/products",product("Brown Rice","BAGS",20,10),t),201);
        var ambiguous=json(request("POST","/assistant/interpret",new JSONObject().put("text","Add 2 bags rice"),t),200);
        assertThat(ambiguous.getJSONArray("candidates").length()).isEqualTo(2);assertThat(ambiguous.isNull("confirmationId")).isTrue();
        var selected=ambiguous.getJSONObject("command").put("productId",ambiguous.getJSONArray("candidates").getJSONObject(0).getString("id"));
        assertThat(json(request("POST","/assistant/preview",selected,t),200).isNull("confirmationId")).isFalse();
        assertThat(json(request("POST","/assistant/preview",chosen,register().token()),200).isNull("confirmationId")).isTrue();
    }
    @Test void businessActionsUseRecordedUsagePersistAndNeverChangeStock() throws Exception {
        var owner=register(); String t=owner.token();
        String rice=json(request("POST","/products",product("Rice","BAGS",40,10),t),201).getString("id");
        json(request("POST","/inventory/remove",stock(rice,14,"BAGS"),t),200);json(request("POST","/inventory/remove",stock(rice,14,"BAGS"),t),200);
        UUID productId=UUID.fromString(rice);
        jdbc.update("UPDATE products SET created_at=current_timestamp - interval '8 days' WHERE id=?",productId);
        var ids=jdbc.queryForList("SELECT id FROM inventory_transactions WHERE product_id=? AND transaction_type='REMOVE' ORDER BY created_at",UUID.class,productId);
        jdbc.update("UPDATE inventory_transactions SET created_at=current_timestamp - interval '6 days' WHERE id=?",ids.get(0));
        jdbc.update("UPDATE inventory_transactions SET created_at=current_timestamp - interval '1 day' WHERE id=?",ids.get(1));
        var center=json(request("GET","/business",null,t),200);var insight=center.getJSONArray("insights").getJSONObject(0);
        assertThat(insight.getDouble("recentUnitsRemoved")).isEqualTo(28);assertThat(insight.getDouble("averageDailyUsage")).isEqualTo(4);assertThat(insight.getDouble("daysRemaining")).isEqualTo(3);
        assertThat(insight.getString("status")).isEqualTo("RUNNING_OUT_SOON");assertThat(insight.getDouble("suggestedReorderQuantity")).isEqualTo(16);
        var approved=json(request("POST","/business/approve/"+rice,null,t),200);String actionId=approved.getString("id");
        assertThat(approved.getString("status")).isEqualTo("APPROVED");
        assertThat(json(request("POST","/business/approve/"+rice,null,t),200).getString("id")).isEqualTo(actionId);
        assertThat(json(request("GET","/business",null,t),200).getJSONArray("actions").length()).isEqualTo(1);
        String other=register().token();assertThat(request("POST","/business/approve/"+rice,null,other).statusCode()).isEqualTo(404);
        assertThat(request("POST","/business/actions/"+actionId+"/complete",null,other).statusCode()).isEqualTo(404);
        assertThat(json(request("GET","/business",null,other),200).getJSONArray("actions").length()).isZero();
        for(String text:List.of("What needs my attention?","What should I reorder?","What is running out soon?","Why should I reorder rice?","How long will rice last?")) {
            var answer=json(request("POST","/assistant/interpret",new JSONObject().put("text",text),t),200);
            assertThat(answer.getJSONArray("insights").getJSONObject(0).getDouble("daysRemaining")).isEqualTo(3);
        }
        var pending=json(request("POST","/assistant/interpret",new JSONObject().put("text","What actions are pending?"),t),200);
        assertThat(pending.getJSONArray("actions").length()).isEqualTo(1);
        var completed=json(request("POST","/business/actions/"+actionId+"/complete",null,t),200);
        assertThat(completed.getString("status")).isEqualTo("COMPLETED");assertThat(completed.isNull("completedAt")).isFalse();
        assertThat(json(request("GET","/business",null,t),200).getJSONArray("actions").getJSONObject(0).getString("status")).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT status FROM business_actions WHERE id=?",String.class,UUID.fromString(actionId))).isEqualTo("COMPLETED");
        assertThat(json(request("GET","/products/"+rice,null,t),200).getDouble("currentStock")).isEqualTo(12);
        assertThat(json(request("GET","/transactions",null,t),200).getInt("totalElements")).isEqualTo(3);
    }
}

