package practica3;

import DBA.SuperAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase que define al agente, su comportamiento, sensores y comunicaciones
 * 
 * @author Adrián Ruiz López
 * @author David Infante Casas
 * @author Mariana Orihuela Cazorla
 * @author Yang Chen
 */
public class Interlocutor extends SuperAgent {
    
    
    /**
     * Estado actual del agente.
     */
    Estados estado;
    /**
     * Mapa que recorre el agente.
     */
    String mapaActual;
    /**
     * Clave de sesión para hacer login y logout.
     */
    String key;
    /**
     * Dimensiones y alturas del mapa.
     */
    int dimX, dimY, alturaMin, alturaMax;
    
    /**
     * Bandeja de entrada y salida de mensajes.
     */
    ACLMessage outbox = null;
    ACLMessage inbox = null;
    
    /**
     * Booleano que indica si el agente empieza la partida (hostea) o no.
     */

    boolean hosting;
    
    /**
     * Conversation ID.
     */
    
    String cId = "";
   
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @param mapa Mapa que va a recorrer el agente
     * @throws Exception
     * 
     * @author Adrián Ruiz Lopez
     */
    public Interlocutor(AgentID aid, String mapa, boolean host) throws Exception {
        super(aid);
        this.hosting = host;
        mapaActual = mapa;
    }
    
    
    
    /**
     * Inicialización del agente
     * 
     * @author Adrian Ruiz Lopez
     * @author David Infante Casas
     */
    @Override
    public void init() {

    }
    
    
    
    /**
     * Comportamiento del agente
     * 
     * @author Mariana Orihuela Cazorla
     */
    @Override
    public void execute() {        
 
        //bandejas de entrada y salida

        
        //if hosteo..
        //intentamos suscribirnos al mundo
        login();
        
        try {
            inbox = receiveACLMessage();
        } catch (InterruptedException ex) {
            Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("No se puede recibir el mensaje");
        }
        
        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
            System.out.println("\nSe ha podido hacer login con éxito");
            outbox = new ACLMessage();
            outbox.setSender(this.getAid());
            outbox.setReceiver(new AgentID("Elnath"));
            outbox.setPerformative(ACLMessage.CANCEL);
            outbox.setConversationId(cId);
            this.send(outbox);
            
            try {
                inbox = receiveACLMessage();
            } catch (InterruptedException ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("No se puede recibir el mensaje");
            }
            
            if(inbox.getPerformativeInt() == ACLMessage.AGREE)
                System.out.println("\nSe ha cerrado sesión");
            else
                System.out.println("\nNo se ha cerrado sesión");
        }
        else{
            System.out.println("\nNo se ha podido hacer login con éxito");
        }
    }
    
    
    /**
     * Envío del mensaje para hacer login
     * 
     * 
     * @author Adrian Ruiz Lopez
     */
    public void login() {
        /* Preparación del Mensaje */
        JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("map",mapaActual);
        objetoJSON.add("user", "Eagle");
        objetoJSON.add("password", "Hzrwtags");
        
        String mensaje = objetoJSON.toString();
        
        /* envío */
        outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Elnath"));
        outbox.setPerformative(ACLMessage.SUBSCRIBE);
        outbox.setContent(mensaje);
        this.send(outbox);
    }
    
    /**
     * Finalización del agente y llamada al método que extrae la traza de ejecución
     * 
     * @author Mariana Orihuela Cazorla
     * @author Adrian Ruiz Lopez
     */
    @Override
    public void finalize() {
        super.finalize();
 
    }
    
        
    
    
}
