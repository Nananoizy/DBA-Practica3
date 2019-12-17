package practica3;

import DBA.SuperAgent;
import DBAMap.DBAMap;
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
import java.util.LinkedList;
import java.util.Queue;
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
    
    boolean tengoObjetivo;
    Queue<Pair<Integer,Integer>> AlemanesPendientes = new LinkedList<Pair<Integer,Integer>> ();
    Pair<Integer,Integer> objetivoActual;
     
    boolean rescatado=false;
    
    String comando="";
   
    
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
        tengoObjetivo = false;
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
            posActualX = posInicioX;
            posActualY = posInicioY;
            posActualZ = consultaAltura(posActualX,posActualY);
            
            
            mandaMensaje(nombreInterlocutor, ACLMessage.CONFIRM , "");
            
            checkIn(objeto);
            
        }
        
        recibeMensaje("todos los drones levantados");
       
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM) online = true;
        else online = false;
        
        
        //cargarPercepciones();
       // posActualZ = gps.get("z").asInt();
        
        while(online){
            
            recibeMensaje("recibiendo un mensaje");
           // this.replyWth = inbox.getReplyWith();

            //Obtenemos quien es el SENDER:
            String sender = inbox.getSender().name;
            
            // SI RECIVE UN ALEMAN, LO PONE EN SU LISTA
            if( sender.equals(nombreInterlocutor)  ){
                // Obtenemos las coordenadas y lo metemos en la cola del rescate:
                JsonObject aleman = Json.parse(inbox.getContent()).asObject();
                Pair<Integer,Integer> nuevoAleman = new Pair(aleman.get("alemanX").asInt(),aleman.get("alemanY").asInt());
                AlemanesPendientes.add(nuevoAleman);
  
                objetivoActual = AlemanesPendientes.poll();
                if(!tengoObjetivo) tengoObjetivo=true;
                
                comando = obtenerComando();
                realizarMovimiento(comando);
                
              //  recibeMensaje("Efectua movimiento " + nombreDron);
                
                
            }else if( sender.equals("Elnath")){
                this.replyWth = inbox.getReplyWith();
                // DISTINGUIR SI LA RESPUESTA RECIBIDA ES DE UN MOVIMIENTO O DE EXITO DE RESCATE:
                if( inbox.getPerformativeInt() == ACLMessage.INFORM ){
                    System.out.println("Soy el " + nombreDron + " y me he movido al: " + comando);
                
                

                    if( tengoObjetivo ){
                        // MOVIMIENTO Y COMUNICACION CON EL CONTROLADOR PERO EL ULTIMO RECIBE MENSAJE NO SE HACE! ( YA QUE SE VOLVERA A HACER EN EL INICIO DEL BUCLE )
                        // CUANDO RESCATE A UN ALEMAN, PONER LA VARIABLE TENGOOBJETIVO A FALSE.
                        /*
                            1º deicidirMovimiento / accion rescatar
                            2º EnviarMov al controlador ( o mensaje de rescate ) 
                        */
                      comando=obtenerComando();
                      realizarMovimiento(comando);
                      
                      if(comando.equals("rescue")){
                          rescatado = true;
                          tengoObjetivo = false;
                      }
                      else if(comando.equals("stop")){
                          tengoObjetivo = false;
                      }
                        
                    }else if( !tengoObjetivo && rescatado && AlemanesPendientes.size()>0 ){
                        // ASIGNA EL NUEVO OBJETIVO
                        objetivoActual = AlemanesPendientes.poll();
                        tengoObjetivo = true;
                        // MOVERSE:
                        comando = obtenerComando();
                        realizarMovimiento(comando);
                    }
                    else if(!tengoObjetivo && rescatado && AlemanesPendientes.isEmpty()){
                        // ASIGNA EL PUNTO DE INICIO COMO OBJETIVO
                        objetivoActual = new Pair(posInicioX,posInicioY);
                        tengoObjetivo = true;
                        //MOVERSE
                        comando=obtenerComando();
                        realizarMovimiento(comando);
                    }
                    
                }// FIN IF SI ES UNA RESPUESTA DE UN MOV.
                else{
                     JsonObject respuesta = Json.parse(inbox.getContent()).asObject();
                     String resp = respuesta.get("result").asString();
                     System.out.println("Soy el " + nombreDron + " y no me he podido mover");
                     System.out.println(resp);
                     
                     online = false;
                     tengoObjetivo = false;
                }
            }
            
            System.out.println(nombreDron + " : Estoy en la posicion x = " + posActualX + " , y = " +posActualY + " , mi altura es " + posActualZ);
            System.out.println("Y el aleman a rescatar esta en  x = " + objetivoActual.getKey() + " , y = " + objetivoActual.getValue() );
                              
        } // FIN DEL WHILE ONLINE
    }
    
    
    
    public void realizarMovimiento(String comando){
        JsonObject objeto = new JsonObject();
                      
        objeto.add("command", comando);
        String content = objeto.toString();
        mandaMensaje("Elnath", ACLMessage.REQUEST, content);
        System.out.println(nombreDron+ " : voy a hacer: " + comando);
        
        actualizaPosicion(comando);
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
            if(posActualZ>=consultaAltura(posActualX,posActualY-1)){
                siguientePos="moveN";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("NE")){
            if(posActualZ>=consultaAltura(posActualX+1,posActualY-1)){
                siguientePos="moveNE";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("E")){
            if(posActualZ>=consultaAltura(posActualX+1,posActualY)){
                siguientePos="moveE";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("SE")){
            if(posActualZ>=consultaAltura(posActualX+1,posActualY+1)){
                siguientePos="moveSE";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("S")){
            if(posActualZ>=consultaAltura(posActualX,posActualY+1)){
                siguientePos="moveS";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("SW")){
            if(posActualZ>=consultaAltura(posActualX-1,posActualY+1)){
                siguientePos="moveSW";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("W")){
            if(posActualZ>=consultaAltura(posActualX-1,posActualY)){
                siguientePos="moveW";
            }
            else{
                siguientePos="moveUP";
            }
        }
        else if (direccion.equals("NW")){
            if(posActualZ>=consultaAltura(posActualX-1,posActualY-1)){
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
        
        if(posActualX==objetivoActual.getKey()){  
            if(posActualY<objetivoActual.getValue()){
                pos="moveS";
            }
            else if(posActualY>objetivoActual.getValue()){
                pos="moveN";
            }
        }
        else if(posActualX<objetivoActual.getKey()){
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
     * El comportamiento del dron rescate
     * 
     * 
     * @author Yang Chen
     */
     public String obtenerComando(){
         
          //actualizamos la posicion que estamos
       /*  cargarPercepciones();
         posActualX = gps.get("x").asInt();
         posActualY = gps.get("y").asInt();
         posActualZ = gps.get("z").asInt();*/
                 
         //si estamos en la casilla de aleman
         if(posActualX == objetivoActual.getKey() && posActualY == objetivoActual.getValue()){
             //si estamos en el suelo y el aleman no ha sido rescatado, hacemos rescate
             if(posActualZ == consultaAltura(posActualX,posActualY) && !rescatado){
               comando="rescue";   
            }
            
            //si no estamos en el suelo, bajamos
             else if(!rescatado){
                 comando = "moveDW";
             } 
         }
         //si ya hemos rescatado los alemanes y estamos en la posicion inicial
         else if(posActualX == posInicioX && posActualY == posInicioY && rescatado){
             //si estamos en el suelo y rescatado, hacemos stop
             if(posActualZ == consultaAltura(posActualX,posActualY)){
                 comando="stop";
             }
             //no estamos en el suelo, bajamos
             else{
                 comando="moveDW";
             }
         }
         //hacemos movimiento normal
         else{
             comando=siguientePosicion();
             }
         return comando;
     }
         
    
    /**
     * obtener angulo a partir de la posicion de un aleman
     * 
     * 
     * @author Yang Chen
     */
    public double obtenerAngulo(int objetivoX, int objetivoY){
          double angulo = 0.0;
          angulo=(double) Math.toDegrees(Math.atan2(objetivoY-posActualX, objetivoX-posActualY));
          
          if(angulo<0){
              angulo+=360;
          }
          
          return angulo;
      }
    
  /*
     public void actualizaPos(String direccion){
        
        if (direccion.equals("moveN")){
            posActualY = posActualY - 1;
        }
        else if (direccion.equals("moveNE")){
            posActualX = posActualX + 1;
            posActualY = posActualY - 1;
        }
        else if (direccion.equals("moveE")){
            posActualX = posActualX + 1;
        }
        else if (direccion.equals("moveSE")){
            posActualX = posActualX + 1;
            posActualY = posActualY + 1;
        }
        else if (direccion.equals("moveS")){
            posActualY = posActualY + 1;
        }
        else if (direccion.equals("moveSW")){
            posActualX = posActualX - 1;
            posActualY = posActualY + 1;
        }
        else if (direccion.equals("moveW")){
            posActualX = posActualX - 1;
        }
        else if (direccion.equals("moveNW")){
            posActualX = posActualX - 1;
            posActualY = posActualY - 1;
        }
        else if (direccion.equals("moveUP")){
            posActualZ = posActualZ + 1;
        }
        else if (direccion.equals("moveDW")){
            posActualZ = posActualZ - 1;
        }
        
        //System.out.println("Posicion actualizada: " + posActualX + " , " + posActualY);
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
