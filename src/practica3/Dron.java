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
 * Clase abstracta Dron que define los comportamientos básicos y variables
 * 
 * @author David Infante Casas
 * @author Mariana Orihuela Cazorla
 */
public abstract class Dron extends SuperAgent {
    
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
     * Dimensiones y alturas del mapa.
     */
    int posInicioX, posInicioY;
    /**
     * Rol del dron.
     */
    String rol;
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
     * @throws Exception
     * 
     * @author David Infante Casas
     */
    public Dron(AgentID aid, boolean host) throws Exception {
        super(aid);
        this.hosting = host;
        mapaActual = new ArrayList<Integer>();  
    }
    
    
    
    /**
     * Inicialización del agente
     * 
     * @author Adrian Ruiz Lopez
     * @author David Infante Casas
     */
    @Override
    public abstract void init();
    
    
    
    /**
     * Comportamiento del agente
     * 
     * @author Mariana Orihuela Cazorla
     */
    @Override
    public abstract void execute();
    
    
    
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
    
    
    
    /**
     * Método que crea un mensaje que se manda
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public void mandaMensaje(String receptor, int performativa, String content) {
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
    
    public void recibeMensaje() {
        try {
            inbox = receiveACLMessage();
        } catch (InterruptedException ex) {
            Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("No se puede recibir el mensaje");
        }
    }
    
}
