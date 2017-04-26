/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author marek
 */
public class Main {
    
    static final Logger logger = Logger.getLogger("PnodeLog");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // NASTAVENIE LOGOV:
            FileHandler fh = new FileHandler("PNnodeLogFile.log");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            // Pripojenie do databazy
            //Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Node", "node", "mysql");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Node", "root", "mysql");
            connection.setSchema("Node");
            
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setAutomaticReconnect(true);
            connOpts.setCleanSession(false);
            connOpts.setUserName("processnode");
            connOpts.setPassword("1processNode".toCharArray());
            String[] serversURIs = new String[2];
            serversURIs[0] = "tcp://localhost:1883";
            serversURIs[1] = "tcp://test.mosquitto.org:1883";
            if(args.length == 2){
                serversURIs[0] = "tcp://" + args[0] + ":" + args[1];
            }
            connOpts.setServerURIs(serversURIs);
            
            PNode pnode = new PNode(connOpts);
            pnode.connectToBroker();
            Worker worker = new Worker(pnode, connection);
            pnode.start_subscribe(worker);
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        System.out.println("Exit signal!");
                        pnode.disconnectFromBroker();
                    } catch (MqttException ex) {
                        logger.log(Level.SEVERE, ex.toString());
                    }
                }
            });
        } catch (SQLException | MqttException ex) {
            logger.log(Level.SEVERE, ex.toString());
        } catch (IOException | SecurityException ex) {
            System.out.println(ex.toString());
        }
    }
    
}
