/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.socraticgrid.websocket;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tnguyen
 */
@ServerEndpoint("/sendmsg")
public class SendMsg {

    private static final Logger logger = Logger.getLogger(SendMsg.class.getName());
    
    private static final String GUEST_PREFIX = "Guest";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    
    private static final Map<SendMsg, String> conns = new HashMap<SendMsg, String>();
    
    private final String nickname;
    private Session session;

    public SendMsg() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
    }


    @OnOpen
    public void start(Session session) {
        this.session = session;
        
        String s = String.format("==> OPENING SOCKET: %s %s %s", nickname, "has joined: ", this.toString());
        logger.log(Level.FINE, "{0}", s);
        System.out.println(s);
    }


    @OnClose
    public void end() {
        conns.remove(this);
        
        String s = String.format("==> CLOSING SOCKET: %s %s", nickname, "has disconnected.");
        logger.log(Level.FINE, "{0}", s);
        System.out.println(s);
        
    }


    @OnMessage
    public void incoming(String message) {
        String s = null;
        //----------------------------------------------
        //     BROADCAST alerts if alerts are given
        //     (note:  will have to trim off headers "ALERTS=###,"
        // else
        //     REGISTER session and patientId
        //----------------------------------------------
        if (message.startsWith("ALERTS=")) {
            s = String.format("==> ALERTS BEING SENT:\n %s", message);
            logger.log(Level.FINE,"{0}", message);
            System.out.println(s);
            
            int commaIndex = message.indexOf(',');
            String patientId = message.substring(7, commaIndex);
            
            String alertsToSend = message.substring(commaIndex+1);
            broadcast(patientId, alertsToSend);
            
        } else {
            
            //---------------------------------------------------------
            // Have to find and remove any previous session of same ID, 
            // cause this should be the new (and only) re-registration
            // of this sesionId-patientId  combo.
            //---------------------------------------------------------
            for (Map.Entry<SendMsg, String> entry : conns.entrySet()) {
               
                SendMsg key = entry.getKey();
                String foundPatientId = entry.getValue();
                
                if (key.toString().equals(this.toString())) {
                    conns.remove(key);
                    
                    s = String.format(
                            "  REMOVING SESSION: %s PID=%s   .. Total Sessions: %s", 
                            key.toString(), foundPatientId, conns.size());
                    System.out.println(s);
                    logger.log(Level.FINE,"==> {0}", s);
                }
            }
            conns.put(this, message.toString());
            
            s = String.format(
                    "SAVING SESSION: %s PID= %s   .. Total Sessions: %s", 
                    this.toString(), message.toString(), conns.size());
            System.out.println(s);
            logger.log(Level.FINE,"==> {0}", s);
        }
        
        
    }


    private void broadcast(String patientId, String msg) {

        //===============================================================
        // LOOP through all registered-still-active session, and if 
        // the patientId of that saved session is same as what is being
        // notified, then broadcast this neew alert to that session.
        //===============================================================
        for (Map.Entry<SendMsg, String> entry : conns.entrySet()) {

            // GET PATIENT ID THIS SAVED SESSION
            SendMsg key = entry.getKey();
            String foundPatientId = entry.getValue();

            System.out.println("=======================================");
            System.out.println("INCOMING SESSION "+ this.toString() + "   and PID: "+ patientId);
            System.out.println("preSAVED SESSION "+ key             + "   and PID: "+foundPatientId);
            
            // SEND this new alert to ONLY session that has patient Id
            if (patientId.equalsIgnoreCase(foundPatientId)) {

                System.out.println("==> FOUND ... sending alert.");
                try {
                    // SEND alert notice
                    key.session.getBasicRemote().sendText(msg);

                } catch (IOException ignore) {
                    // Ignore
                }
            }

        }

    }//end-broadcast

        
}

