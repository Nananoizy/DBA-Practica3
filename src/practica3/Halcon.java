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
        
<<<<<<< Updated upstream
        if (online){
           //La primera vez, pedimos percepciones por primera vez:
            cargarPercepciones();
            obtenerAlemanesInfrarojos();
        }
                
        // Una vez se ha inicializado continuamos en el bucle:
        while( online ){
=======

                
        // Una vez se ha inicializado continuamos en el bucle:
        while( online ) {
            cargarPercepciones();
            obtenerAlemanesInfrarojos();
            if( coordAleman.size() == 0  ) obtenerAlemanGonio();
            mostrarArrayAlemanes();
            
>>>>>>> Stashed changes
            
            // SI NO TIENE UNA POSICION INDICADA O LA POSICION INDICADA ES LA ACTUAL, PETIDMOS NUEVA POS
            if (((nextPosX == -1) || (nextPosY == -1)) || ((posActualX == nextPosX) && (posActualY == nextPosY))){
                pedirSiguientePosicion();
                recibeMensaje("Recibir siguiente posicion");
                
                JsonObject objeto = Json.parse(inbox.getContent()).asObject();            
                nextPosX = objeto.get("irAX").asInt();
                nextPosY = objeto.get("irAY").asInt();
                
<<<<<<< Updated upstream
                System.out.println("La siguiente posicion a ir es: " + nextPosX + " , " + nextPosY);
=======
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
            else if( coordAleman.size() == 0 ) {
                String siguienteDireccion = "";
                siguienteDireccion = calculaDireccion();
                
                JsonObject objeto = new JsonObject();
                
                ///Si no tengo fuel suficiente, reposto. Else me muevo
                // Calculamos el número de pasos que necesitamos para bajar al suelo
                int numero_pasos_bajar = this.posActualZ - this.consultaAltura(this.posActualX, this.posActualY);
                // Hacemos refuel
                if (fuel-(numero_pasos_bajar*fuelrate) < 15.0) {
                    this.refuel(siguienteDireccion, numero_pasos_bajar);
                }else{
                    
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
   
>>>>>>> Stashed changes
            }
            
            mandarCoordenadas();
            
<<<<<<< Updated upstream
            // SI TIENE POSICION INDICADA Y NO ES LA POSICION ACTUAL
                // COMPROBAMOS SI TIENE ALEMANES EN SU RADAR
                    // SI TIENE ALEMANES, MANDA UN MENSAJE AL INTERLOCUTOR Y ESPERA A QUE LE CONTESTE
                    // SI NO TIENE ALEMANES, AVANZA
            online = false;
=======
            
            // ESPERAR LAS RESPUESTAS DE LOS RESCATES:
            while( coordAleman.size() > 0 ){
                recibeMensaje("recibiendo un aleman rescatado");
                if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                    //OBTENER QUE ALEMAN ES EL QUE HA SIDO RESCATADO Y QUITARLO DEL ARRAY.
                    JsonObject aleman = Json.parse(inbox.getContent()).asObject();
                    int posx= aleman.get("posX").asInt();
                    int posy= aleman.get("posY").asInt();
                    Pair<Integer,Integer> alem = new Pair<Integer,Integer>(posx,posy);
                    // si ese aleman, lo habiamos detectado con el halcon, lo eliminamos
                    if( coordAleman.contains(alem) ){
                        coordAleman.remove(alem);
                    }
                    
                }
            }
            
>>>>>>> Stashed changes
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
                objeto = Json.parse(inbox.getContent()).asObject();
                System.out.println("Checkin halcon: " + objeto.get("result").asString());
                posActualX = posInicioX;
                posActualY = posInicioY;
                
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
    

    
    
    
    
}// FIN CLASE HALCON
