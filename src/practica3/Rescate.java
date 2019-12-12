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
public class Rescate extends Dron {
    
    boolean tengoObjetivo=false;
    
     ArrayList<Pair<Integer,Integer>> coordenadaAleman = new ArrayList<Pair<Integer,Integer>> ();
     
     Pair<Integer,Integer> objetivo;
     
     int miX, miY, miZ;
     
     boolean rescatado=false;
   
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @throws Exception
     * 
     * @author David Infante Casas
     */
    public Rescate(AgentID aid, boolean host, String nombreArchivo) throws Exception {
        super(aid, host,nombreArchivo);
        rol = "rescue";
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
 
        recibeMensaje("primer mensaje en levantar la rescate");
        
        //Si se ha recibido un mensaje con la performativa inform, actualizamos los valores de nuestras variables
        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
            this.cId = inbox.getConversationId();
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();
            sessionKey = objeto.get("session").asString();

            posInicioX = objeto.get("posInicioX").asInt();
            posInicioY = objeto.get("posInicioY").asInt();
            dimX = objeto.get("dimMaxX").asInt();
            dimY = objeto.get("dimMaxY").asInt();
            
            mandaMensaje(nombreInterlocutor, ACLMessage.CONFIRM , "");
            
            checkIn(objeto);
            
        }
        
        recibeMensaje("todos los drones levantados");
       
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            online = true;
        }
        else
            online = false;
        
        
        while(online){
            
            
            Pair<Integer,Integer> aux=new Pair(54,76);
            objetivo=aux;
            tengoObjetivo=true;
            
            while(tengoObjetivo){
                cargarPercepciones();
                miX=gps.get("x").asInt();
                miY=gps.get("y").asInt();
                miZ=gps.get("z").asInt();
                String siguientePos = "";
                
                JsonObject objeto = new JsonObject();
                
                //Si estoy en la casilla de aleman
                if(miX==objetivo.getKey() && miY==objetivo.getValue()){
                    //si estoy en el suelo y el aleman no ha sido rescatado, rescato
                    if(miZ==0 && !rescatado){//if(miZ==consultaAltura(miX,miY) && !rescatado){
                         objeto.add("command","rescue");
                         String content = objeto.toString();
                         mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                         System.out.println("performativa enviado es " + outbox.getPerformative().toString());
                         
                        recibeMensaje("Efectua rescue el dron rescate");
                        this.replyWth = inbox.getReplyWith();
                         System.out.println("performativa recibido es " + inbox.getPerformative().toString());
                         
                        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                             objeto = Json.parse(inbox.getContent()).asObject();
                             System.out.println("El aleman " + objeto.get("id").asString() + " ha sido rescatado.");
                             rescatado=true;
                          }
                        else{
                            JsonObject respuesta = Json.parse(inbox.getContent()).asObject();            
                        String resp = respuesta.get("result").asString();
                        System.out.println("Soy el rescate y no me he podido hacer rescue");
                        System.out.println(resp);
                        online = false;
                        tengoObjetivo=false;
                        }
                        
                        //si ha sido rescatado el leman, tengo que volver, asgno la coordenada inicial como mi objetivo
                          if(rescatado){
                              objetivo=new Pair(posInicioX ,posInicioY);
                           }
                    }
                    // si no estoy en el suelo, bajo
                    else{
                        siguientePos="moveDW";
                        objeto.add("command",siguientePos);
                        String content = objeto.toString();
                        mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                    
                         System.out.println("rescate: Me quiero mover a: " + siguientePos);
                    
                         recibeMensaje("Efectua movimiento rescate");
                         this.replyWth = inbox.getReplyWith();
                         
                         if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                         System.out.println("Soy el rescate y me he movido al: " + siguientePos);
                         
                        
                        

                    }
                    else{
                        JsonObject respuesta = Json.parse(inbox.getContent()).asObject();            
                        String resp = respuesta.get("result").asString();
                        System.out.println("Soy el rescate y no me he podido mover");
                        System.out.println(resp);
                        online = false;
                        tengoObjetivo=false;
                    }
                         
                    }
                }
                //si estoy en la posicion inicial
                else if(miX==posInicioX&&miY==posInicioY && rescatado){
                    //si estoy en el suelo y rescatado, hago stop
                    if(miZ==consultaAltura(miX,miY) ){
                         objeto.add("command","stop");
                         String content = objeto.toString();
                         mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                    
                        recibeMensaje("Efectua stop el dron rescate");
                      //  this.replyWth = inbox.getReplyWith();
                        
                        tengoObjetivo=false;
                        System.out.println("rescate: ya he rescatado el aleman y he hecho el stop.");
                    }
                    /*else{
                        objeto.add("command","moveDW");
                         String content = objeto.toString();
                         mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                    
                        recibeMensaje("Efectua movimiento el dron rescate");
                        this.replyWth = inbox.getReplyWith();
                    }*/
                }
                else{
                    siguientePos=siguientePosicion();
                     objeto.add("command",siguientePos);
                    String content = objeto.toString();
                    mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                    System.out.println("el cid es " + outbox.getConversationId().toString());
                    
                    System.out.println("rescate: Me quiero mover a: " + siguientePos);
                    
                    recibeMensaje("Efectua movimiento rescate");
                    this.replyWth = inbox.getReplyWith();
                    
                    if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                         System.out.println("Soy el rescate y me he movido al: " + siguientePos);
                         
                        
                        

                    }
                    else{
                        JsonObject respuesta = Json.parse(inbox.getContent()).asObject();            
                        String resp = respuesta.get("result").asString();
                        System.out.println("Soy el rescate y no me he podido mover");
                        System.out.println(resp);
                        online = false;
                    }
                    
                }
                
                System.out.println("rescate : estoy en X= " + miX + " y= " + miY + " mi altura es " + miZ);
                System.out.println("rescate : y aleman esta en X= " + objetivo.getKey() + " y= " + objetivo.getValue());
                
             // actualizaPos(siguientePos);
                
            }//Fin de while (tengo objetivo)
           
            
        }
    }
    
    public void checkIn(JsonObject objeto){
        // Check in
            JsonObject objetoJSON = new JsonObject();
            objetoJSON.add("command", "checkin");
            objetoJSON.add("session", sessionKey);
            objetoJSON.add("rol", this.rol);
            objetoJSON.add("x", posInicioX);
            objetoJSON.add("y", posInicioY);
            String content = objetoJSON.toString();
            mandaMensaje("Elnath", ACLMessage.REQUEST , content);
            
            // Respuesta al check in
            recibeMensaje("mensaje de check in de rescate");
            
            if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
                this.cId = inbox.getConversationId();
                this.replyWth = inbox.getReplyWith();
                objeto = Json.parse(inbox.getContent()).asObject();
                //System.out.println("rescate: " + objeto.get("result").asString());
                
                datosCheckin();
                
                mandaMensaje(nombreInterlocutor, ACLMessage.CONFIRM, "rescate");
                
                //Si todo ha ido bien, esperamos que el interlocutor nos diga hacia donde movernos
                //recibeMensaje();
                  
            } else if (inbox.getPerformativeInt() == ACLMessage.FAILURE) {
                mandaMensaje(nombreInterlocutor, ACLMessage.FAILURE, "rescate");
                System.out.println("Error FAILURE\n");
            } else if (inbox.getPerformativeInt() == ACLMessage.REFUSE) {
                mandaMensaje(nombreInterlocutor, ACLMessage.REFUSE, "rescate");
                System.out.println("Error REFUSE\n");
            } else if (inbox.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
                mandaMensaje(nombreInterlocutor, ACLMessage.NOT_UNDERSTOOD, "rescate");
                System.out.println("Error NOT UNDERSTOOD\n");
            }
    }
    
   /*  public String siguientePosicion(){
         String siguientePos="";
         
         String direccion = "";
         
         double angulo=obtenerAngulo(objetivo.getKey(),objetivo.getValue());
         direccion=obtenerDireccion(angulo);
         
          if (direccion.equals("N")){
            if(miZ>=consultaAltura(miX,miY-1)){
                siguientePos="moveN";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("NE")){
            if(miZ>=consultaAltura(miX+1,miY-1)){
                siguientePos="moveNE";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("E")){
            if(miZ>=consultaAltura(miX+1,miY)){
                siguientePos="moveE";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("SE")){
            if(miZ>=consultaAltura(miX+1,miY+1)){
                siguientePos="moveSE";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("S")){
            if(miZ>=consultaAltura(miX,miY+1)){
                siguientePos="moveS";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("SW")){
            if(miZ>=consultaAltura(miX-1,miY+1)){
                siguientePos="moveSW";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("W")){
            if(miZ>=consultaAltura(miX-1,miY)){
                siguientePos="moveW";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("NW")){
            if(miZ>=consultaAltura(miX-1,miY-1)){
                siguientePos="moveNW";
            }
            else{
                siguientePos="moveUP";
            }
        }
   
         
            
         return siguientePos;
     }*/
    
    /**
     * calcula la siguiente posicion a mover
     * 
     * 
     * @author Yang Chen
     */
    public String siguientePosicion(){
        String pos="";
        
        if(miX==objetivo.getKey()){  
            if(miY<objetivo.getValue()){
                pos="moveS";
            }
            else if(miY>objetivo.getValue()){
                pos="moveN";
            }
        }
        else if(miX<objetivo.getKey()){
            pos="moveE";
        }
        else{
            pos="moveW";
        }
        
        return pos;
    }
     
    /**
     * Obtener direccion a dirigir a partir de un angulo
     * 
     * 
     * @author Yang Chen
     */
     public String obtenerDireccion(double angulo){
         String direccion="";
         
          if(angulo >= 337.5 || angulo < 22.5) direccion="N";
          else if (angulo >= 22.5 && angulo < 67.5) direccion="NE";
          else if (angulo >= 67.5 && angulo < 112.5) direccion="E";
          else if (angulo >= 112.5 && angulo < 157.5) direccion="SE";
          else if (angulo >= 157.5 && angulo < 202.5) direccion="S";
          else if (angulo >= 202.5 && angulo < 247.5) direccion="SW";
          else if (angulo >= 247.5 && angulo < 292.5) direccion="W";
          else if (angulo >= 292.5 && angulo < 337.5) direccion=" NW";
              
         return direccion;
     }
    
    
    /**
     * obtener angulo a partir de la posicion de un aleman
     * 
     * 
     * @author Yang Chen
     */
    public double obtenerAngulo(int objetivoX, int objetivoY){
          double angulo = 0.0;
          angulo=(double) Math.toDegrees(Math.atan2(objetivoY-miX, objetivoX-miY));
          
          if(angulo<0){
              angulo+=360;
          }
          
          return angulo;
      }
    
  /*
     public void actualizaPos(String direccion){
        
        if (direccion.equals("moveN")){
            miY = miY - 1;
        }
        else if (direccion.equals("moveNE")){
            miX = miX + 1;
            miY = miY - 1;
        }
        else if (direccion.equals("moveE")){
            miX = miX + 1;
        }
        else if (direccion.equals("moveSE")){
            miX = miX + 1;
            miY = miY + 1;
        }
        else if (direccion.equals("moveS")){
            miY = miY + 1;
        }
        else if (direccion.equals("moveSW")){
            miX = miX - 1;
            miY = miY + 1;
        }
        else if (direccion.equals("moveW")){
            miX = miX - 1;
        }
        else if (direccion.equals("moveNW")){
            miX = miX - 1;
            miY = miY - 1;
        }
        else if (direccion.equals("moveUP")){
            miZ = miZ + 1;
        }
        else if (direccion.equals("moveDW")){
            miZ = miZ - 1;
        }
        
        //System.out.println("Posicion actualizada: " + miX + " , " + miY);
    }
    
    */
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
