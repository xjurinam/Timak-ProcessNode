package tp;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author marek
 */
public class Order {
    
    private int orderId;
    private String uuid;
    private int clientId;
    private String accountNumber;
    private String mail;
    private String topic;
    private List<Product> products;

    public Order() {
        this.uuid = String.valueOf(UUID.randomUUID());
        this.clientId = -1;
        this.accountNumber = "";
        this.mail = "";
        this.products = new ArrayList();
        this.orderId = -1;
        this.topic = "";
    }

    public String getUuid() {
        return uuid;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getUsernameFromTopic(){
        if(this.topic.contains("buyMultiple"))
            return this.topic.replace("/pn/request/buyMultiple/", "");
        else
            return this.topic.replace("/pn/request/buy/", "");
    }
    
    public String toInsertMySqlString(){
        return "INSERT INTO orders "
                + "(uuid, username, mail) "
                + "VALUES ("
                + "\"" + this.uuid + "\", "
                + "\"" + this.getUsernameFromTopic() + "\", "
                + "\"" + this.mail + "\""
                + ");";
    }
}