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
public class Mosca extends Dron {
   
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @throws Exception
     * 
     * @author David Infante Casas
     */
    public Mosca(AgentID aid, boolean host) throws Exception {
        super(aid, host);
        rol = "fly";
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
 
        recibeMensaje();
              
        //Si se ha recibido un mensaje con la performativa inform, actualizamos los valores de nuestras variables
        if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
            this.cId = inbox.getConversationId();
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();
            sessionKey = objeto.get("session").asString();

            posInicioX = objeto.get("posInicioX").asInt();
            posInicioY = objeto.get("posInicioY").asInt();
            dimX = objeto.get("dimMaxX").asInt();
            dimY = objeto.get("dimMaxY").asInt();
            
            mandaMensaje("Grupoe", ACLMessage.CONFIRM , "");
            
            // Check in
            JsonObject objetoJSON = new JsonObject();
            objetoJSON.add("command", "checkin");
            objetoJSON.add("session", "master");
            objetoJSON.add("rol", this.rol);
            objetoJSON.add("x", posInicioX);
            objetoJSON.add("y", posInicioY);
            mandaMensaje("Elnath", ACLMessage.REQUEST , "");
            
            // Respuesta al check in
            recibeMensaje();
            
            if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
                this.cId = inbox.getConversationId();
                objeto = Json.parse(inbox.getContent()).asObject();
                System.out.println(objeto.get("result").asString());
            } else if (inbox.getPerformativeInt() == ACLMessage.FAILURE) {
                System.out.println("Error FAILURE\n");
            } else if (inbox.getPerformativeInt() == ACLMessage.REFUSE) {
                System.out.println("Error REFUSE\n");
            } else if (inbox.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
                System.out.println("Error NOT UNDERSTOOD\n");
            }            
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
