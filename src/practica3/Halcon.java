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
import javafx.util.Pair;
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
        nombreDron = "halcon";
        rescatando = true;
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
                 
        }
        
        //esperamos a que el interlocutor nos confirme que todos los drones se han levantado bien
        recibeMensaje("todos los drones levantados");
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM) online = true;
        else online = false;
        
        // Una vez se ha inicializado continuamos en el bucle:
        while( online ){
            cargarPercepciones();
            obtenerAlemanesInfrarojos();
            
            //obtenerAlemanGonio();

            // SI NO TIENE UNA POSICION INDICADA O LA POSICION INDICADA ES LA ACTUAL, PETIDMOS NUEVA POS
            if (((nextPosX == -1) || (nextPosY == -1)) || ((posActualX == nextPosX) && (posActualY == nextPosY))){
                pedirSiguientePosicion();
                recibeMensaje("Recibir siguiente posicion");
                
                JsonObject objeto = Json.parse(inbox.getContent()).asObject();
                
                // si los dos son -1, es la señal de stop
                if (objeto.get("irAX").asInt() == -1 && objeto.get("irAY").asInt() == -1){
                    //System.out.println("El halcón va a detenerse");
                    pideParar();
                }
                else{
                    nextPosX = objeto.get("irAX").asInt();
                    nextPosY = objeto.get("irAY").asInt();
                }
                
                //System.out.println("La siguiente posicion a ir es: " + nextPosX + " , " + nextPosY);
            }
            else{
                String siguienteDireccion = "";
                siguienteDireccion = calculaDireccion();
                
                JsonObject objeto = new JsonObject();
                
                ///Si no tengo fuel suficiente, reposto. Else me muevo     
                // Calculamos el número de pasos que necesitamos para bajar al suelo
                int numero_pasos_bajar = (this.posActualZ - this.consultaAltura(this.posActualX, this.posActualY)) / 5;                // Hacemos refuel
                if (fuel-(numero_pasos_bajar*fuelrate) < 15.0) {
                    this.refuel(siguienteDireccion, numero_pasos_bajar);
                }
                else{
                    
                    objeto.add("command",siguienteDireccion);
                    String content = objeto.toString();

                    //System.out.println("Me quiero mover a: " + siguienteDireccion);

                    mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                    //System.out.println(replyWth);
                    recibeMensaje("Efectua movimiento halcon");
                    this.replyWth = inbox.getReplyWith();
                    if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                         //System.out.println("Soy el halcon y me he movido al: " + siguienteDireccion);
                         //Si se mueve a una determinada casilla, habra que actualizar la posActual segun su movimiento
                         fuel = fuel - fuelrate;
                         
                         //actualizamos la posicion localmente
                         actualizaPosicion(siguienteDireccion);
                    }
                    else{
                        JsonObject respuesta = Json.parse(inbox.getContent()).asObject();            
                        String resp = respuesta.get("result").asString();
                        System.out.println("Soy el halcon y no me he podido mover");
                        System.out.println(resp);
                        online = false;
                    }
                }
                
                
                
            }
            
            mandarInformacionPercepciones();
            
            // este while va a estar recibiendo mensajes hasta que el halcon no tenga alemanes en su Array
            while( coordAleman.size() > 0 ){
                recibeMensaje("recibir aleman rescatado");
                JsonObject objeto = Json.parse(inbox.getContent()).asObject();
                int x = objeto.get("posX").asInt();
                int y = objeto.get("posY").asInt();
                Pair<Integer,Integer> aleman = new Pair<Integer,Integer>(x,y);
                if( coordAleman.contains(aleman) ) coordAleman.remove(aleman);
            } 
            
           
            
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
            recibeMensaje("mensaje de checkIN de halcon");
            
            if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
                this.cId = inbox.getConversationId();
                this.replyWth = inbox.getReplyWith();
                //System.out.println(replyWth);
                objeto = Json.parse(inbox.getContent()).asObject();
                System.out.println("Checkin halcon: " + objeto.get("result").asString());
                posActualX = posInicioX;
                posActualY = posInicioY;
                posActualZ=consultaAltura(posActualX,posActualY);
                
                datosCheckin();
                //Enviamos al interlocutor que el check si ha sido correcto.
                mandaMensaje(nombreInterlocutor, ACLMessage.CONFIRM, "halcon");
            } else if (inbox.getPerformativeInt() == ACLMessage.FAILURE) {
                System.out.println("Error FAILURE\n");
                this.replyWth = inbox.getReplyWith();
                mandaMensaje(nombreInterlocutor, ACLMessage.FAILURE, "halcon");
            } else if (inbox.getPerformativeInt() == ACLMessage.REFUSE) {
                System.out.println("Error REFUSE\n");
                this.replyWth = inbox.getReplyWith();
                mandaMensaje(nombreInterlocutor, ACLMessage.REFUSE, "halcon");
            } else if (inbox.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
                System.out.println("Error NOT UNDERSTOOD\n");
                this.replyWth = inbox.getReplyWith();
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
    

    
    
    
    
}// FIN CLASE HALCON
