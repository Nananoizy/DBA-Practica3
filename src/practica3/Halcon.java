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
public class Halcon extends Dron {
       
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @throws Exception
     * 
     * @author Adrián Ruiz Lopez
     */
    public Halcon(AgentID aid, boolean host,String nombreArchivo) throws Exception {
        super(aid, host,nombreArchivo);
        rol = "hawk";
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
 
        recibeMensaje(" primer mensaje en levantar el halcon");
        
        //Si se ha recibido un mensaje con la performativa inform, actualizamos los valores de nuestras variables
        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
            this.cId = inbox.getConversationId();
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();
            sessionKey = objeto.get("session").asString();

            posInicioX = objeto.get("posInicioX").asInt();
            posInicioY = objeto.get("posInicioY").asInt();
            dimX = objeto.get("dimMaxX").asInt();
            dimY = objeto.get("dimMaxY").asInt();
            
            // Confirmamos al interlocutor que ha recibido la orden.
            mandaMensaje(nombreInterlocutor, ACLMessage.CONFIRM , "");
            
            // Mandamos el mesnaje al interlocutor y al controlador
            checkIn(objeto);
            online = true;
            
        }
        
        //La primera vez, pedimos percepciones:
        cargarPercepciones();
                
        // Una vez se ha inicializado continuamos en el bucle:
        while( online ){
            
            // SI NO TIENE UNA POSICION INDICADA O LA POSICION INDICADA ES LA ACTUAL, PETIDMOS NUEVA POS
            // SI TIENE POSICION INDICADA Y NO ES LA POSICION ACTUAL
                // COMPROBAMOS SI TIENE ALEMANES EN SU RADAR
                    // SI TIENE ALEMANES, MANDA UN MENSAJE AL INTERLOCUTOR Y ESPERA A QUE LE CONTESTE
                    // SI NO TIENE ALEMANES, AVANZA
            online = false;
        }       
        
        
        
        
    }
    
    
    
    /**
     * Manda el mensaje de check in al interlocutor y al controlador
     * 
     * @author Mariana Orihuela Cazorla
     * @author Adrian Ruiz Lopez
     */
    public void checkIn( JsonObject objeto ){
        
            // Check in
            JsonObject objetoJSON = new JsonObject();
            objetoJSON.add("command", "checkin");
            objetoJSON.add("session", sessionKey);
            objetoJSON.add("rol", this.rol);
            objetoJSON.add("x", posInicioX);
            objetoJSON.add("y", posInicioY);
            
            String content = objetoJSON.toString();
            // Enviamos al controlador la peticion de check in
            mandaMensaje("Elnath", ACLMessage.REQUEST , content);
            
            // Respuesta al check in
            recibeMensaje("mensaje se checkIN de halcon");
            
            if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
                this.cId = inbox.getConversationId();
                this.replyWth = inbox.getReplyWith();
                objeto = Json.parse(inbox.getContent()).asObject();
                
                datosCheckin();
                //Enviamos al interlocutor que el check si ha sido correcto.
                mandaMensaje(nombreInterlocutor, ACLMessage.CONFIRM, "halcon");
                   
            } else if (inbox.getPerformativeInt() == ACLMessage.FAILURE) {
                System.out.println("Error FAILURE\n");
                mandaMensaje(nombreInterlocutor, ACLMessage.FAILURE, "halcon");
            } else if (inbox.getPerformativeInt() == ACLMessage.REFUSE) {
                System.out.println("Error REFUSE\n");
                mandaMensaje(nombreInterlocutor, ACLMessage.REFUSE, "halcon");
            } else if (inbox.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
                System.out.println("Error NOT UNDERSTOOD\n");
                mandaMensaje(nombreInterlocutor, ACLMessage.NOT_UNDERSTOOD, "halcon");
            }
    }
    
    /**
     * Realiza las percepciones con el controlador
     * 
     * @author Adrian Ruiz Lopez
     */
    public void cargarPercepciones(){
        mandaMensaje("Elnath", ACLMessage.QUERY_REF ,"");
        recibeMensaje("mensaje de pedirPercepciones");
        
        if(inbox.getPerformativeInt() == ACLMessage.INFORM ){
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();
            JsonObject result =  objeto.get("result").asObject();
            gps = result.get("gps").asObject();
            infrared = result.get("infrared").asArray();
            gonio = result.get("gonio").asObject();
            fuel = result.get("fuel").asInt();
            goal = result.get("goal").asBoolean();
            status = result.get("status").asString();
            awacs = result.get("awacs").asArray();
            torescue = result.get("torescue").asInt();
            energy = result.get("energy").asDouble();
            cancel =result.get("cancel").asBoolean();
            /*
            System.out.println("GPS -> "+gps);
            System.out.println("INFRAROJOS -> "+infrared);
            System.out.println("GONIO -> "+ gonio);
            System.out.println("FUEL -> "+ fuel);
            System.out.println("GOAL -> "+ goal);
            System.out.println("STATUS -> "+ status);
            System.out.println("AWACS -> "+ awacs);
            System.out.println("TORESCUE -> "+ torescue);
            System.out.println("ENERGY -> "+ energy);
            System.out.println("CANCEL -> "+ cancel);
            */            
        }
        
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
