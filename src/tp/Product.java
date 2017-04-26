/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import java.util.UUID;

/**
 *
 * @author marek
 */
public class Product {
    
    private int id;
    private int amount;
    private int idFromShop;
    private String reservationUuid;
    private int state;
    
    public Product(){
        this.id = -1;
        this.amount = -1;
        this.idFromShop = -1;
        this.state = -1;
        this.reservationUuid = String.valueOf(UUID.randomUUID());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getIdFromShop() {
        return idFromShop;
    }

    public void setIdFromShop(int idFromShop) {
        this.idFromShop = idFromShop;
    }

    public String getReservationUuid() {
        return reservationUuid;
    }

    public void setReservationUuid(String reservationUuid) {
        this.reservationUuid = reservationUuid;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
    
}
