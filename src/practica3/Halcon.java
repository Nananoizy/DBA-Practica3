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
import java.util.List;
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
        
        //La primera vez, pedimos percepciones por primera vez:
        cargarPercepciones();
        obtenerAlemanesInfrarojos();
                
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
     * Finalización del agente y llamada al método que extrae la traza de ejecución
     * 
     * @author Mariana Orihuela Cazorla
     * @author Adrian Ruiz Lopez
     */
    @Override
    public void finalize() {
        super.finalize();
 
    }
    
    
    

    public void obtenerAlemanesInfrarojos(){
        List<JsonValue> lista = infrared.values();
        
        int numeroAlemanesDetectados = 0;
        for( int i=0;i<lista.size();i++){
           if ( lista.get(i).asInt() == 1){
            numeroAlemanesDetectados++;
            }
        }
        
        List<Integer> posi = new ArrayList<Integer>();
        
        for( int y=0;y<41;y++){
            for(int x=0;x<41;x++){
                if( lista.get((y*40)+x).asInt() == 1 ){
                       posi.add(x);
                       posi.add(y);
                }
            }
        }
        System.out.println(gps);
        System.out.println(posi);
        
        for(int i=0; i<posi.size();i+=2){
            int x = posi.get(i);
            int y = posi.get(i+1);
            
            if( x<20 && y<20 ){
                x = gps.get("x").asInt() - x;
                y = gps.get("y").asInt() - y;
            }else if ( x>20 && y<20){
                x = gps.get("x").asInt() + x;
                y = gps.get("y").asInt() - y;
            }else if ( x<20 && y>20 ){
                x = gps.get("x").asInt() - x;
                y = gps.get("y").asInt() + y;
            }else if ( x>20 && y>20 ){
                x = gps.get("x").asInt() + x;
                y = gps.get("y").asInt() + y;
            }else if ( x==20 ){
                x = gps.get("x").asInt();
                if( y < 20 ){
                    y = gps.get("y").asInt() - y;
                }else if ( y > 20 ){
                    y = gps.get("y").asInt() + y;
                }else{
                    y = gps.get("y").asInt();
                }
            }else if ( y==20 ){
                y = gps.get("y").asInt();
                if( x < 20 ){
                    x = gps.get("x").asInt() - x;
                }else if ( x > 20 ){
                    x = gps.get("x").asInt() + x;
                }else{
                    x = gps.get("x").asInt();
                }
            }
            
            System.out.println("Aleman -> (" + x + "," + y + ")" );
            
        }
        
        
        
        
       



    }
    
    
    
    
}
