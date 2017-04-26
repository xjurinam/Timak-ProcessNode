/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.eclipse.paho.client.mqttv3.MqttException;
import static tp.Main.logger;

/**
 *
 * @author marek
 */
public class Worker implements IMqttNode{
    
    private PNode pnode;
    private Connection connection;
    private List<Order> orders;

    public Worker() {
    }

    public Worker(PNode pnode, Connection connection) {
        this.pnode = pnode;
        this.connection = connection;
        this.orders = new ArrayList();
    }

    public PNode getPnode() {
        return pnode;
    }

    public void setPnode(PNode pnode) {
        this.pnode = pnode;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    
    public ResultSet executeSQL(String sql) throws SQLException{
        Statement stmt = this.connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        stmt.close();
        return rs;
    }
    
    public void updateSQL(String sql) throws SQLException{
        Statement stmt = this.connection.createStatement();
        stmt.executeLargeUpdate(sql);
        stmt.close();
    }
    
    public void spracujShopConfig(String json){
        String merchantName = null;
        try{
            JsonReader reader = Json.createReader(new StringReader(json));
            JsonObject object = reader.readObject();
            String messageId = object.getString("messageId");
            String messageType = object.getString("messageType");
            String senderUuid = object.getString("senderUuid");
            JsonObject payload = object.getJsonObject("payload");
            merchantName = payload.getString("merchantName");
            
            Statement stmt = this.connection.createStatement();
            String sql = "SELECT * FROM merchants WHERE uuid='"+senderUuid+"';";
            ResultSet rs = stmt.executeQuery(sql);
            int idShop = 0;
            while(rs.next()){
                    idShop  = rs.getInt("id_shop");
            }
            if(idShop != 0){
                sql = "UPDATE merchants SET name='"+merchantName+"', is_active=1 WHERE id_shop="+idShop+";";
                stmt.executeUpdate(sql);
            }
            System.out.println("Shop "+merchantName+" configuration ... [OK]");
            sendTopicsForClient();
        } catch (SQLException ex) {
            System.out.println("Shop "+merchantName+" configuration ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void spracujProducts(String json){
            String nameShop = null;
        try{
            JsonReader reader = Json.createReader(new StringReader(json));
            JsonObject object = reader.readObject();
            String messageId = object.getString("messageId");
            String messageType = object.getString("messageType");
            String senderUuid = object.getString("senderUuid");
            
            Statement stmt = this.connection.createStatement();
            String sql = "SELECT id_shop, name, is_active FROM merchants WHERE uuid='"+senderUuid+"';";
            ResultSet rs = stmt.executeQuery(sql);
            int idShop = 0;
            int isActive = 0;
            while(rs.next()){
                    idShop  = rs.getInt("id_shop");
                    nameShop = rs.getString("name");
                    isActive = rs.getByte("is_active");
            }
            if(idShop != 0){
                JsonArray payload = object.getJsonArray("payload");
                for(int i = 0; i != payload.size(); i++){
                    object = payload.getJsonObject(i);
                    String productName = object.getString("productName");
                    JsonNumber productPrice = object.getJsonNumber("productPrice");
                    int amount = object.getInt("amount");
                    int productId = object.getInt("productId");
                    //int isActive = object.getInt("isActive");
                    
                    sql = "SELECT id_product FROM products WHERE id_shop="+idShop+" AND product_id="+ productId+";";
                    rs = stmt.executeQuery(sql);
                    rs.last();
                    int numberOfResults = rs.getRow();
                    if(numberOfResults == 0){
                        sql = "INSERT INTO products "
                                + "(name, id_shop, product_id,price, amount, is_active) "
                                + "VALUES (\""+productName+"\", "
                                + idShop + ", "
                                + productId + ", "
                                + productPrice.doubleValue() + ", "
                                + amount + ", "
                                + isActive+");";
                    }
                    else{
                        sql = "UPDATE products SET "
                                + "name=\"" + productName + "\","
                                + "id_shop=" + idShop + ","
                                + "product_id=" + productId + ","
                                + "price=" + productPrice.doubleValue() + ","
                                + "amount=" + amount + ","
                                + "is_active=" + isActive
                                + " WHERE id_shop=" + idShop
                                + " AND product_id=" + productId + ";";
                    }
                    // System.out.println("SQL: "+sql);
                    stmt.executeUpdate(sql);
                }
                System.out.println("Shop "+nameShop+" products inserting ... [OK]");
                sendProductsToTopic(idShop, nameShop);
            }
            /*if(isActive != 1){
                sendTopicsForClient();
            }*/
        } catch (SQLException ex) {
            System.out.println("Shop "+nameShop+" products inserting ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public List<String> getAllMerchantNameFromDatabase() throws SQLException{
        Statement stmt = this.connection.createStatement();
        String sql = "SELECT name FROM merchants WHERE is_active=1;";
        ResultSet rs = stmt.executeQuery(sql);
        List<String> shopNames = new ArrayList();
        while(rs.next()){
            shopNames.add(rs.getString("name"));
        }
        rs.close();
        stmt.close();
        return shopNames;
    }
    
    public void sendTopicsForClient(){
        try {
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            for(String name : getAllMerchantNameFromDatabase()){
                builder.add(Json.createObjectBuilder().add("topic", name));
            }
            JsonObject object = factory.createObjectBuilder()
                    .add("allTopics", builder)
                    .build();
            pnode.sendMessage(IMqttNode.pubAllTopics, object.toString(), true);
            System.out.println("Sending allTopics ... [OK]");
        } catch (SQLException | MqttException ex) {
            System.out.println("Sending allTopics ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void sendProductsToTopic(int shopId, String shopName){
        String sql = "SELECT * FROM products WHERE id_shop=" + shopId
                + " AND is_active=1 ;";
        try(Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            while(rs.next()){
                JsonObjectBuilder object = Json.createObjectBuilder();
                object.add("id", rs.getString("product_id"));
                object.add("name", rs.getString("name"));
                object.add("price", rs.getString("price"));
                object.add("available", rs.getString("amount"));
                builder.add(object);
            }
            rs.close();
            JsonObject object = factory.createObjectBuilder()
                    .add(shopName, builder)
                    .build();
            pnode.sendMessage(shopName, object.toString(), true);
            System.out.println("Sending products to "+ shopName +" topic ... [OK]");
        } catch (SQLException | MqttException ex) {
            System.out.println("Sending products to "+ shopName +" topic ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void executeBuyOrder(String topic, String json){
        JsonReader reader = Json.createReader(new StringReader(json));
        JsonObject object = reader.readObject();
        Order order = new Order();
        order.setClientId(object.getInt("id"));
        order.setAccountNumber(object.getString("accountNumber"));
        order.setMail(object.getString("mail"));
        order.setTopic(topic);
        Product product = new Product();
        product.setId(object.getInt("product"));
        product.setAmount(object.getInt("amount"));
        order.getProducts().add(product);
        this.orders.add(order);
        saveOrderToDatabase(order);
        reserveProductsInShops(order);
    }
    
    public void executeMultipleBuyOrder(String topic, String json){
        JsonReader reader = Json.createReader(new StringReader(json));
        JsonObject object = reader.readObject();
        Order order = new Order();
        order.setClientId(object.getInt("id"));
        order.setAccountNumber(object.getString("accountNumber"));
        order.setMail(object.getString("mail"));
        order.setTopic(topic);
        JsonArray array = object.getJsonArray("products");
        List<Product> products = order.getProducts();
        for(int i = 0; i < array.size(); i++){
            JsonObject obj = array.getJsonObject(i);
            Product product = new Product();
            product.setId(obj.getInt("product"));
            product.setAmount(obj.getInt("amount"));
            products.add(product);
        }
        this.orders.add(order);
        saveOrderToDatabase(order);
        reserveProductsInShops(order);
    }
    
    public void saveOrderToDatabase(Order order) {
        try(Statement stmt = this.connection.createStatement()){
            this.updateSQL(order.toInsertMySqlString());
            String sql = "Select id_order From orders WHERE uuid=\""
                    + order.getUuid() + "\";";
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                order.setOrderId(rs.getInt("id_order"));
            }
            
            for(Product product : order.getProducts()){
                sql = "INSERT INTO orders_products "
                        + "(id_order, id_product, amount) VALUES ( "
                        + order.getOrderId() + ", "
                        + product.getId() + ", "
                        + product.getAmount()
                        + " )";
                this.updateSQL(sql);
                // Zistenie id produktu v obchode z databazy a ulozenie do pamate
                sql = "SELECT product_id FROM products WHERE id_product="
                    + product.getId() + ";";
                rs = stmt.executeQuery(sql);
                while(rs.next()){
                    product.setIdFromShop(rs.getInt("product_id"));
                }
                rs.close();
            }
            System.out.println("Buy order "+ order.getOrderId() +"  ... [OK]");
        } catch (SQLException ex) {
            System.out.println("Buy order "+ order.getOrderId() +"  ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void reserveProductsInShops(Order order){
        try(Statement stmt = this.connection.createStatement()){
            String sql = "";
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            for(Product product : order.getProducts()){
                JsonObject object = factory.createObjectBuilder()
                    .add("messageId", String.valueOf(UUID.randomUUID()))  // message id
                    .add("messageType", "00000011") // message type
                    .add("senderUuid", IMqttNode.PNodeUUID) // sender uuid
                    .add("payload", Json.createObjectBuilder()
                            .add("orderId", order.getOrderId())
                            .add("productId", product.getIdFromShop())
                            .add("amount", product.getAmount())
                            .add("reservationUuid", product.getReservationUuid()))
                    .build();
                sql = "SELECT m.name FROM merchants as m "
                        + "INNER JOIN products as p "
                        + "ON m.id_shop=p.id_shop "
                        + "WHERE p.id_product=\""
                        + product.getId() + " LIMIT 1\";";
                ResultSet rs = stmt.executeQuery(sql);
                String shopName = "";
                while(rs.next()){
                    shopName = rs.getString("name");
                }
                rs.close();
                this.pnode.sendMessage(IMqttNode.pubProductReservation + shopName, object.toString(), false);
            }
            System.out.println("Products reservation order "+ order.getOrderId() +"  ... [OK]");
        } catch (SQLException | MqttException ex) {
            System.out.println("Products reservation order "+ order.getOrderId() +"  ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void executeAvailability(String json){
        try{
            JsonReader reader = Json.createReader(new StringReader(json));
            JsonObject object = reader.readObject();
            int orderId = object.getInt("orderId");
            int success = object.getInt("success");
            String reservationUuid = object.getString("reservationUuid");
            Order order2 = null;
            boolean conctactBank = true;
            synchronized(this) {
                for(Order order : orders){
                    if(order.getOrderId() == orderId){
                        order2 = order;
                        for(Product product : order.getProducts()){
                            if(product.getReservationUuid().equals(reservationUuid)){
                                product.setState(success);
                            }
                            if(product.getState() != 1){
                                conctactBank = false;
                            }
                        }
                        break;
                    }
                }
            }
            if(conctactBank)
                doPayment(order2);
            if(success == 0){
                contactUserWithResponse("Product unavailable!", success, order2);
                synchronized(this) {
                    this.orders.remove(order2);
                }
            }
        }catch(Exception ex){
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void doPayment(Order order){
        try (Statement stmt = this.connection.createStatement()){
            Calendar calendar = Calendar.getInstance();
            java.sql.Timestamp timestamp = new java.sql.Timestamp(calendar.getTime().getTime());
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonArrayBuilder paymentDestinations = Json.createArrayBuilder();
            String sql = "SELECT CONCAT(p.name,\" \",o.amount,\"x - \",TRUNCATE(o.amount*p.price,2),\" EUR\")as message "
                    + "FROM orders_products as o "
                    + "INNER JOIN products as p ON o.id_product=p.id_product "
                    + "WHERE id_order="
                    + order.getOrderId() +";";
            String message = "";
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                if(message.endsWith(","))
                    message += rs.getString("message");
                else
                    message += rs.getString("message") + ",";
            }
            sql = "SELECT SUM(truncate(o.amount * p.price,2)) as amount, i.iban "
                    + "FROM orders_products as o "
                    + "INNER JOIN products as p ON o.id_product=p.id_product "
                    + "INNER JOIN shops_ibans as i ON i.id_shop=p.id_shop "
                    + "WHERE id_order=" + order.getOrderId()
                    + " GROUP BY p.id_shop;";
            rs = stmt.executeQuery(sql);
            while(rs.next()){
                JsonObjectBuilder object = Json.createObjectBuilder();
                object.add("destinationAccount", rs.getString("iban"));
                object.add("amount", rs.getString("amount"));
                paymentDestinations.add(object);
            }
            String bankId = order.getAccountNumber().substring(4, 8);
            JsonObject object = factory.createObjectBuilder()
                    .add("paymentId", order.getUuid())
                    .add("bankId", bankId)
                    .add("sourceAccount", order.getAccountNumber())
                    .add("currency", "EUR")
                    .add("time_sent", timestamp.toString())
                    .add("message", message)
                    .add("paymentDestinations", paymentDestinations)
                    .build();
            
            this.pnode.sendMessage("/bank/" + bankId + "/payment_orders", object.toString(), false);
            System.out.println("Send payment to bank, order "+ order.getOrderId() +"  ... [OK]");
        } catch (MqttException | SQLException ex) {
            System.out.println("Send payment to bank, order "+ order.getOrderId() +"  ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void exexuteBankResponse(String json){
        Order order2 = null;
        try {
            /*
            ● pending - čakajúca platba, pridá sa vždy hneď po odoslaní
            ● received - úspešne prevedená platba
            ● expired - vypršaná platba, po uplynutí času bez overenia
            ● rejected - odmietnuta platba, napríklad pri odmietnutí autentifikovania sa
            */
            JsonReader reader = Json.createReader(new StringReader(json));
            JsonObject object = reader.readObject();
            String paymentId = object.getString("paymentId");
            String status = object.getString("status");
            if(status.equals("pending")){
                System.out.println("Bank response pending!");
                return;
            }
            int success = 0;
            if(status.equals("received"))
                success = 1;
            synchronized(this) {
                for(Order order : orders){
                    if(order.getUuid().equals("paymentId")){
                        order2 = order;
                        break;
                    }
                }
            }
            System.out.println("Received payment from bank, order "+ order2.getOrderId() +"  ... [OK]");
            contactUserWithResponse("Payment "+status, success, order2);
            synchronized(this) {
                this.orders.remove(order2);
            }
        }catch(Exception ex){
            System.out.println("Received payment from bank, order "+ order2.getOrderId() +"  ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void contactUserWithResponse(String message, int success, Order order){
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObject obj = factory.createObjectBuilder()
                .add("id", order.getClientId())
                .add("success", success)
                .add("message", message)
                .build();
        String topic = "";
        if(order.getTopic().contains("buyMultiple"))
            topic = "/pn/response/buyMultiple/" + order.getUsernameFromTopic();
        else
            topic = "/pn/response/buy/" + order.getUsernameFromTopic();
        try {
            this.pnode.sendMessage(topic, obj.toString(), false);
            System.out.println("Contacting client with response: order "+ order.getOrderId() +"  ... [OK]");
        } catch (MqttException ex) {
            System.out.println("Contacting client with response: order "+ order.getOrderId() +"  ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
    
    public void contactShopWithResponse(String message, int success, Order order){
        try(Statement stmt = this.connection.createStatement()) {
            String sql = "";
            for(Product product : order.getProducts()){
                sql = "SELECT * FROM products as p"
                        + "INNER JOIN shops as s ON s.id_shop=p.id_shop "
                        + "WHERE ";
                product.getId();
            }
            this.pnode.sendMessage("", "", false);
            System.out.println("Contacting shop with response: order "+ order.getOrderId() +"  ... [OK]");
        } catch (Exception ex) {
            System.out.println("Contacting shop with response: order "+ order.getOrderId() +"  ... [FAIL]");
            System.err.println(ex.toString());
        }
    }
    
    public void sendHistory(String topic, String json){
        String username = topic.replace("/pn/request/history/", "");
        topic = "/pn/response/history/" + username;
        int i = 9999;
        try(Statement stmt = this.connection.createStatement()){
            JsonReader reader = Json.createReader(new StringReader(json));
            JsonObject object = reader.readObject();
            if(JsonValue.ValueType.NUMBER == object.get("last").getValueType())
                if(object.getInt("last") != -1)
                    i = object.getInt("last");
            String sql = "SELECT m.name as merchant, "
                    + "p.name as product, p.price as price, op.amount as amount, "
                    + "DATE_FORMAT(o.timestamp,'%d %m %Y') as date "
                    + "FROM orders_products as op "
                    + "INNER JOIN products as p ON op.id_product=p.id_product "
                    + "INNER JOIN merchants as m ON m.id_shop=p.id_shop "
                    + "INNER JOIN orders as o ON op.id_order=o.id_order "
                    + "WHERE o.username=\"" + username + "\" "
                    + "ORDER BY date DESC "
                    + "LIMIT " + i + ";";
            JsonArrayBuilder builder = Json.createArrayBuilder();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                JsonObjectBuilder obj = Json.createObjectBuilder();
                obj.add("merchant", rs.getString("merchant"));
                obj.add("product", rs.getString("product"));
                obj.add("price", rs.getDouble("price"));
                obj.add("amount", rs.getInt("amount"));
                obj.add("date", rs.getString("date"));
                builder.add(obj);
            }
            this.pnode.sendMessage(topic, 
                    Json.createObjectBuilder().add("products", builder).build().toString(),
                    false);
            System.out.println("Sending history to " + username + "  ... [OK]");
        }catch(Exception ex){
            System.out.println("Sending history to " + username + "  ... [FAIL]");
            logger.log(Level.SEVERE, ex.toString());
        }
    }
}