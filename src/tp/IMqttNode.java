/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;
/**
 *
 * @author marek
 * TP Bezpecne sprostredkovanie online platieb
 */
public interface IMqttNode {
    //static final String subTopic = "shops/"; // za lomitkom pribudne v kode identifikator obchodnika
    static final String subProductsTopic = "pn/products";
    static final String subAvalabilityTopic = "pn/response/products";
    static final String subShopConfigTopic = "pn/shops";
    static final String subBuy = "/pn/request/buy/+";
    static final String subBuyMultiple = "/pn/request/buyMultiple/+";
    static final String subBankResponseTopic = "/bank/payment_order_responses";
    static final String subHistoryTopic = "/pn/request/history/+";
    
    static final String pubAllTopics = "/pn/response/allShops";
    static final String pubProductReservation = "shops/";
    //static final String pubPayment = "/bank/<bankID>/payment_orders";
    
    static final int QoS = 2;
    static final String PNodeUUID = "process node";//"11111111-2222-3333-1111-222222222222";
}
