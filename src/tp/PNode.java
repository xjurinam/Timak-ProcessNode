/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 *
 * @author marek
 */
public class PNode implements IMqttNode{
    
    private MqttConnectOptions connOpts;
    private MqttAsyncClient client;
    private IMqttToken token;

    public PNode(MqttConnectOptions connOpts) {
        this.connOpts = connOpts;
    }

    public MqttConnectOptions getConnOpts() {
        return connOpts;
    }

    public void setConnOpts(MqttConnectOptions connOpts) {
        this.connOpts = connOpts;
    }

    public MqttAsyncClient getClient() {
        return client;
    }

    public void setClient(MqttAsyncClient client) {
        this.client = client;
    }

    public IMqttToken getToken() {
        return token;
    }

    public void setToken(IMqttToken token) {
        this.token = token;
    }
    
    public void connectToBroker() throws MqttException{
        MemoryPersistence persistance = new MemoryPersistence();
        this.client = new MqttAsyncClient(
                this.connOpts.getServerURIs()[0],
                this.connOpts.getUserName(),
                persistance);
        this.token = client.connect(this.connOpts);
        this.token.waitForCompletion();
        System.out.println("Connecting to broker ... [OK]");
    }
    
    public void disconnectFromBroker() throws MqttException{
        if(this.client.isConnected()) {
            token = this.client.disconnect();
            token.waitForCompletion();
            System.out.println("Disconnecting from broker ... [OK]");
        }
    }
    
    public void sendMessage(String topic, String payload, boolean retain) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setRetained(retain);
        message.setQos(2);
        this.client.publish(topic, message);
        System.out.println((char)27 + "[34mS: "+topic+" : "+payload+ (char)27 + "[0m");
    }
    
    public void start_subscribe(Worker worker) throws MqttException{
        // SPRACOVANIE PRODUKTOV OD SHOPU
        token = this.client.subscribe(IMqttNode.subProductsTopic, 2, new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                // System.out.println("A: "+topic+" : "+m.toString());
                worker.spracujProducts(m.toString());
            }
        });
        
        // SPRACOVANIE KONFIGURACIE SHOPU
        token = this.client.subscribe(IMqttNode.subShopConfigTopic, QoS, new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                // System.out.println("A: "+topic+" : "+m.toString());
                worker.spracujShopConfig(m.toString());
            }
        });
        
        // SPRACOVANIE NAKUPU JEDNEHO PRODKTU
        token = this.client.subscribe(IMqttNode.subBuy, QoS, new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                System.out.println((char)27+"[35mA: "+topic+" : "+m.toString()+ (char)27 + "[0m");
                worker.executeBuyOrder(topic, m.toString());
            }
        });
        
        // SPRACOVANIE NAKUPU VIACERYCH PRODUKTOV
        token = this.client.subscribe(IMqttNode.subBuyMultiple, QoS, new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                System.out.println((char)27+"[35mA: "+topic+" : "+m.toString()+ (char)27 + "[0m");
                worker.executeMultipleBuyOrder(topic, m.toString());
            }
        });
        
        // SPRACOVANIE ODPOVEDE NA REZERVACIU PRODUKTOV
        token = this.client.subscribe(IMqttNode.subAvalabilityTopic, QoS, new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                System.out.println((char)27+"[35mA: "+topic+" : "+m.toString()+ (char)27 + "[0m");
                worker.executeAvailability(m.toString());
            }
        });
        
        // SPRACOVANIE ODPOVEDE OD BANKY
        token = this.client.subscribe(IMqttNode.subBankResponseTopic, QoS, new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                System.out.println((char)27+"[35mA: "+topic+" : "+m.toString()+ (char)27 + "[0m");
                worker.exexuteBankResponse(m.toString());
            }
        });
        
        // SPRACOVANIE HISTORIE PRE KLIENTA
        token = this.client.subscribe(IMqttNode.subHistoryTopic, QoS, new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                System.out.println((char)27+"[35mA: "+topic+" : "+m.toString()+ (char)27 + "[0m");
                worker.sendHistory(topic, m.toString());
            }
        });
    }
    
}
