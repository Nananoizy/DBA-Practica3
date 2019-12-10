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
    public Mosca(AgentID aid, boolean host,String nombreArchivo) throws Exception {
        super(aid, host,nombreArchivo);
        rol = "fly";
        nombreDron = "mosca";
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
 
        recibeMensaje("primer mensaje de levantar mosca");
              
        //Si se ha recibido un mensaje con la performativa inform, actualizamos los valores de nuestras variables
        if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
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
            
        }
        
        //esperamos a que el interlocutor nos confirme que todos los drones se han levantado bien
        recibeMensaje("todos los drones levantados");
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            online = true;
        }
        else
            online = false;
        
        // cargamos las percepciones
        
        if (online){
            cargarPercepciones();
        }
        
        while(online){
            
            
            // SI NO TIENE UNA POSICION INDICADA O LA POSICION INDICADA ES LA ACTUAL, PETIDMOS NUEVA POS
            if (((nextPosX == -1) || (nextPosY == -1))){
                pedirSiguientePosicion();
                recibeMensaje("Recibir siguiente posicion");
                
                JsonObject objeto = Json.parse(inbox.getContent()).asObject();            
                nextPosX = objeto.get("irAX").asInt();
                nextPosY = objeto.get("irAY").asInt();
                
                //System.out.println("La siguiente posicion a ir es: " + nextPosX + " , " + nextPosY);
            }
            // Si la posicion que tiene es su destino final, se espera
            else if (((posActualX == nextPosX) && (posActualY == nextPosY))){
                
            }
            else{
                String siguienteDireccion = "";
                siguienteDireccion = calculaDireccion();
                
                JsonObject objeto = new JsonObject();
                
                ///Si no tengo fuel suficiente, reposto. Else me muevo     
                if (fuel <= fuelrate + 2){
                    objeto.add("command","refuel");
                    String content = objeto.toString();
                    mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                    
                    recibeMensaje("Efectua refuel mosca");
                    this.replyWth = inbox.getReplyWith();
                    
                    if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                         System.out.println("La mosca ha hecho refuel");
                         cargarPercepciones();
                    }
                }
                else{
                    
                    objeto.add("command",siguienteDireccion);
                    String content = objeto.toString();

                    //System.out.println("Me quiero mover a: " + siguienteDireccion);

                    mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                    //System.out.println(replyWth);
                    recibeMensaje("Efectua movimiento mosca");
                    this.replyWth = inbox.getReplyWith();
                    if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                         System.out.println("Soy la mosca y me he movido al: " + siguienteDireccion);
                         //Si se mueve a una determinada casilla, habra que actualizar la posActual segun su movimiento
                         fuel = fuel - fuelrate;
                         
                         //actualizamos la posicion localmente
                         actualizaPosicion(siguienteDireccion);
                    }
                    else{
                        JsonObject respuesta = Json.parse(inbox.getContent()).asObject();            
                        String resp = respuesta.get("result").asString();
                        System.out.println("Soy la mosca y no me he podido mover");
                        System.out.println(resp);
                        online = false;
                    }
                }
                
                
                
            }
            
            ///////////////////////////////////////////
                
            // COMPORTAMIENTO EN TIMEPO DE RESCATE.            
            
            /////////////////////////////////////////////
        }
        
        
    }
    
    
    
    /**
     * Manda el mensaje de check in al interlocutor y al controlador
     * 
     * @author Mariana Orihuela Cazorla
     * @author Adrian Ruiz Lopez
     */
    public void checkIn( JsonObject objeto ){
            JsonObject objetoJSON = new JsonObject();
            objetoJSON.add("command", "checkin");
            objetoJSON.add("session", sessionKey);
            objetoJSON.add("rol", this.rol);
            objetoJSON.add("x", posInicioX);
            objetoJSON.add("y", posInicioY);
            
            String content = objetoJSON.toString();
            mandaMensaje("Elnath", ACLMessage.REQUEST , content);
            
            // Respuesta al check in
            recibeMensaje("mensaje de check in de mosca");
            
            if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
                this.cId = inbox.getConversationId();
                this.replyWth = inbox.getReplyWith();
                objeto = Json.parse(inbox.getContent()).asObject();
                posActualX = posInicioX;
                posActualY = posInicioY;
                System.out.println("Checkin mosca: " + objeto.get("result").asString());
                
                datosCheckin();
                //Enviamos al interlocutor que el check si ha sido correcto.
                mandaMensaje(nombreInterlocutor, ACLMessage.CONFIRM, "mosca");
            } else if (inbox.getPerformativeInt() == ACLMessage.FAILURE) {
                mandaMensaje(nombreInterlocutor, ACLMessage.FAILURE, "mosca");
                System.out.println("Error FAILURE\n");
            } else if (inbox.getPerformativeInt() == ACLMessage.REFUSE) {
                mandaMensaje(nombreInterlocutor, ACLMessage.REFUSE, "mosca");
                System.out.println("Error REFUSE\n");
            } else if (inbox.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
                mandaMensaje(nombreInterlocutor, ACLMessage.NOT_UNDERSTOOD, "mosca");
                System.out.println("Error NOT UNDERSTOOD\n");
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
