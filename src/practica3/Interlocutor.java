package practica3;

import DBA.SuperAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONArray;

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
     * Drones de la práctica.
     */
    
    Halcon halcon;
    Mosca mosca;
    Rescate rescate1, rescate2;
    /**
     * Estado actual del agente.
     */
    Estados estado;
    /**
     * Mapa que recorre el agente.
     */
    ArrayList<Integer> mapaActual;   
    String nombreMapaActual;
    /**
     * Clave de sesión para hacer login y logout.
     */
    String key;
    /**
     * Dimensiones y alturas del mapa.
     */
    int dimX, dimY;
    
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
     * Clave de sesión.
     */
    
    String sessionKey = "";
   
    
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
        nombreMapaActual = mapa;
        mapaActual = new ArrayList<Integer>();  
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
        
        recibeMensaje();
        
        // SI HE CONSEGUIDO SUSCRIBIRME A UN MUNDO
        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
            System.out.println("\nSe ha podido hacer login con éxito");
            
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();  
            cId = inbox.getConversationId();
            sessionKey = objeto.get("session").asString();
            
            ///EXTRAER MAPA
            JsonArray mapArray = objeto.get("map").asArray();
            
            for (int i = 0; i < mapArray.size() ; i++) {
                mapaActual.add( mapArray.get(i).asInt());
            }
            
            //DIMENSIONES DEL MAPA
            dimX = objeto.get("dimx").asInt();
            dimY = objeto.get("dimy").asInt();
            
            try {   
                // Una vez hemos conseguido los datos que nos interesan, informamos a los distintos drones

                levantarDrones();
                
            } catch (Exception ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            cancelarPartida();
            
        }
        else{
            System.out.println("\nNo se ha podido hacer login con éxito");
        }
    }
    
    /**
     * Método que levanta a los drones
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void levantarDrones() throws Exception{
        
        //Creamos los demás drones y les mandamos los datos necesarios para que empiecen a operar
        mosca = new Mosca(new AgentID("Grupoe_mosca"), true);
        halcon = new Halcon(new AgentID("Grupoe_halcon"), true);
        rescate1 = new Rescate(new AgentID("Grupoe_rescate1"), true);
        rescate2 = new Rescate(new AgentID("Grupoe_rescate2"), true);
        
        
        // LEVANTAMOS PRIMERO LA MOSCA
        
        mosca.start();
        
        JsonObject objetoJSONInicio = new JsonObject();
        objetoJSONInicio.add("session", sessionKey);
        String content = objetoJSONInicio.toString();
        
        mandaMensaje("Grupoe_mosca", ACLMessage.INFORM, content);
        
        recibeMensaje();
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado la mosca"); 
        }
        else{
            System.out.println("\nNo se ha levantado la mosca");
            cancelarPartida();
        }
        
        // DESPUES EL HALCON
        
        halcon.start();
        
        objetoJSONInicio = new JsonObject();
        objetoJSONInicio.add("session", sessionKey);
        content = objetoJSONInicio.toString();
        
        mandaMensaje("Grupoe_halcon", ACLMessage.INFORM, content);
        
        recibeMensaje();
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado el halcon"); 
        }
        else{
            System.out.println("\nNo se ha levantado el halcon");
            cancelarPartida();
        }
        
        // Y LOS DOS DRONES DE RESCATE
        
        rescate1.start();
        
        objetoJSONInicio = new JsonObject();
        objetoJSONInicio.add("session", sessionKey);
        content = objetoJSONInicio.toString();
        
        mandaMensaje("Grupoe_rescate1", ACLMessage.INFORM, content);
        
        recibeMensaje();
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado el rescate 1"); 
        }
        else{
            System.out.println("\nNo se ha levantado el rescate 1");
            cancelarPartida();
        }
        
        rescate2.start();
        
        objetoJSONInicio = new JsonObject();
        objetoJSONInicio.add("session", sessionKey);
        content = objetoJSONInicio.toString();
        
        mandaMensaje("Grupoe_rescate2", ACLMessage.INFORM, content);
        
        recibeMensaje();
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado el rescate 2"); 
        }
        else{
            System.out.println("\nNo se ha levantado el rescate 2");
            cancelarPartida();
        }
        
    }
    
    /**
     * Método que crea un mensaje que se manda
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public void mandaMensaje(String receptor, int performativa, String content){
        
        outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID(receptor));
        outbox.setPerformative(performativa);
        outbox.setConversationId(cId);
        outbox.setContent(content);
        this.send(outbox);
        
    }
    
        /**
     * Método que recibe un mensaje
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public void recibeMensaje(){
        
        try {
                inbox = receiveACLMessage();
            } catch (InterruptedException ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("No se puede recibir el mensaje");
            }
        
    }
    
    /**
     * Envío del mensaje para hacer login
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void login() {
        /* Preparación del Mensaje */
        JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("map",nombreMapaActual);
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
     * Envío del mensaje para cancelar la partida
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void cancelarPartida(){
        
        outbox = new ACLMessage();
            outbox.setSender(this.getAid());
            outbox.setReceiver(new AgentID("Elnath"));
            outbox.setPerformative(ACLMessage.CANCEL);
            outbox.setConversationId(cId);
            outbox.setContent("");
            this.send(outbox);
            
            recibeMensaje();
            
        if(inbox.getPerformativeInt() == ACLMessage.AGREE)
            System.out.println("\nSe ha cerrado sesión");
        else
            System.out.println("\nNo se ha cerrado sesión"); 
            
    }
    
    /**
     * Extracción de la traza del mapa
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void extraerTraza() throws FileNotFoundException, IOException {
        
            JsonObject injson = Json.parse(inbox.getContent()).asObject();
            JsonArray ja = injson.get("map").asArray();
            byte data[] = new byte [ja.size()];
            for (int i = 0; i < data.length; i++){
                data[i] = (byte) ja.get(i).asInt();
            }
            FileOutputStream fos = new FileOutputStream("mitraza.png");
            fos.write(data);
            fos.close();
            System.out.println("Traza guardada");
   
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
