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
public class Mosca extends SuperAgent {
    
    
    /**
     * Estado actual del agente.
     */
    Estados estado;

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
    public Mosca(AgentID aid, boolean host) throws Exception {
        super(aid);
        this.hosting = host;
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
 
        try {
            //nada más ser levantado, el agente mosca va a esperar a que el interlocutor le mande los datos del mundo
            inbox = receiveACLMessage();
        } catch (InterruptedException ex) {
            System.out.println("\nNo se ha podido recibir el mensaje");
        }
        
        //Si se ha recibido un mensaje con la performativa inform, actualizamos los valores de nuestras variables
        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
            System.out.println("\nHe recibido el mensaje");
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
