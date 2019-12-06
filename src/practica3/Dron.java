package practica3;

import DBA.SuperAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
     * Reply with para las conversaciones con el controlador.
     */
    String replyWth = "";
    /**
     * Cantidad de fuel que gasta el dron en cada paso.
     */
    float fuelrate;
    /**
     * Altura maxima a la que puede volar.
     */
    int maxlevel;
    /**
     * Visibilidad del gonio.
     */
    int visibility;
    /**
     * Visibilidad del infrarrojos.
     */
    int range;
    
    /**
     * Mapa Completo.
    */
    BufferedImage mapa;
    
    /**
     * Nombre del interlocutor.
     */
    String nombreInterlocutor = "Grupoe";
    
    /**
     * variable que indica que esta en funcionamiento.
     */
    boolean online;
    
    /**
     * Posicion a la que tiene que ir.
     */
    int nextPosX,nextPosY;
    
    /**
     *  PERCEPCIONES
     */
    JsonObject gps;
    JsonArray infrared;
    JsonArray awacs;
    JsonObject gonio;
    int fuel;
    boolean goal;
    String status; // Y ESTE STATUS?
    int torescue; // ni idea de que es tamapoco
    double energy; // ni idea
    boolean cancel; // ni idea
    
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @throws Exception
     * 
     * @author David Infante Casas
     */
    public Dron(AgentID aid, boolean host, String nombreArchivo) throws Exception {
        super(aid);
        this.hosting = host;
        this.cargarMapa(nombreArchivo);
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
     * Método que interpreta los datos del checkin
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public void datosCheckin(){
        
         JsonObject objeto = Json.parse(inbox.getContent()).asObject();
         
         fuelrate = objeto.get("fuelrate").asFloat();
         maxlevel = objeto.get("maxlevel").asInt();
         visibility = objeto.get("visibility").asInt();
         range = objeto.get("range").asInt();
         
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
        
        if (!replyWth.equals("")){
            outbox.setInReplyTo(replyWth);
        }
        
        outbox.setContent(content);
        this.send(outbox);
    }
    
    /**
     * Método que recibe un mensaje
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public void recibeMensaje( String cadena) {
        try {
            inbox = receiveACLMessage();
        } catch (InterruptedException ex) {
            Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("No se puede recibir el mensaje" + cadena);
        }
    }
    
    
     
    /**
    * Método que carga el mapa de disco
    * 
    * 
    * @author Adrián Ruiz Lopez
    */
    public void cargarMapa( String urlArchivo ) throws IOException{
        mapa = ImageIO.read( new File(urlArchivo) );
    }
    
     
    /**
    * Método que consulta una altura en el mapa
    * 
    * 
    * @author Adrián Ruiz Lopez
    */
    public int consultaAltura(int x, int y){
        int altura = new Color(mapa.getRGB(x, y)).getBlue();
        return altura;
    }
    
    
    /**
    * Método DE EJEMPLO QUE NOS SERVIRÁ DESPUES (SI NO BORRAR)
    * 
    * 
    * @author Adrián Ruiz Lopez
    */
    public void metodo( String dir ){
        
        switch(dir){
            case "N":
                
                break;
            case "NW":
                
                break;
            case "W":
                
                break;
            case "S":
                
                break;
            case "SE":
                
                break;
            case "E":
                
                break;
            case "NE":
                
                break;
            case "SW":
                
                break;
        }
        
        
    }
    
    
    
}
