/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.message;

import java.util.UUID;

/**
 *
 * @author marek
 */
public abstract class MQTTMessage {
    private String uuid;
    private String json;

    public MQTTMessage(String uuid, String json) {
        this.json = json;
        this.uuid = uuid;
    }

    public MQTTMessage(String json){
        this.json = json;
        this.uuid = String.valueOf(UUID.randomUUID());
    }
    
    public MQTTMessage() {
        this.uuid = String.valueOf(UUID.randomUUID());
        this.json = "";
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
}
