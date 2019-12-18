package practica3;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.LinkedList;
import java.util.Queue;
import javafx.util.Pair;

/**
 * Clase que define al agente, su comportamiento, sensores y comunicaciones
 * 
 * @author Adrián Ruiz López
 * @author David Infante Casas
 * @author Mariana Orihuela Cazorla
 * @author Yang Chen
 */
public class Rescate extends Dron {
    
    /**
     * Booleano para denotar si el dron tiene un alemán al que rescatar
     */
    boolean tengoObjetivo;
    /**
     * Lista de alemanes a los que rescatar
     */
    Queue<Pair<Integer,Integer>> AlemanesPendientes = new LinkedList<Pair<Integer,Integer>> ();
    /**
     * Coordenadas del alemán que se va a rescatar a continuación
     */
    Pair<Integer,Integer> objetivoActual;
    /**
     * Comando actual
     */
    String comando="";
   
    
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @param host Gost al que conectar
     * @param nombreArchivo Nombre archivo
     * @throws Exception
     * 
     * @author David Infante Casas
     */
    public Rescate(AgentID aid, boolean host, String nombreArchivo) throws Exception {
        super(aid, host,nombreArchivo);
        rol = "rescue";
        tengoObjetivo = false;
        nombreDron = "rescate";
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
     * @author David Infante Casas
     * @author Yang Chen
     * @author Adrián Ruiz López
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
        
        while(online){
            
           recibeMensaje("recibiendo un mensaje");
           
           if (rescatando){

            //Obtenemos quien es el SENDER:
            String sender = inbox.getSender().name;
            
            // SI RECIVE UN ALEMAN, LO PONE EN SU LISTA
            if(sender.equals(nombreInterlocutor)){
                // Obtenemos las coordenadas y lo metemos en la cola del rescate:
                JsonObject aleman = Json.parse(inbox.getContent()).asObject();
                Pair<Integer,Integer> nuevoAleman = new Pair(aleman.get("alemanX").asInt(),aleman.get("alemanY").asInt());
                AlemanesPendientes.add(nuevoAleman);
                
                if(!tengoObjetivo){
                    objetivoActual = AlemanesPendientes.poll();  
                    tengoObjetivo=true;
                    comando = obtenerComando();
                    realizarMovimiento(comando);
                }                
                
            } else if (sender.equals("Elnath")) {
                this.replyWth = inbox.getReplyWith();
                // DISTINGUIR SI LA RESPUESTA RECIBIDA ES DE UN MOVIMIENTO O DE EXITO DE RESCATE:
                if( inbox.getPerformativeInt() == ACLMessage.INFORM ) {
                    cargarPercepciones();
                    // compruebo si tengo que hacer refuel
                    
                    int numero_pasos_bajar = (this.posActualZ - this.consultaAltura(this.posActualX, this.posActualY)) / 5; 
                    // Hacemos refuel
                    if (fuel-(numero_pasos_bajar*fuelrate) < 15.0) {
                        
                        this.refuel(comando, numero_pasos_bajar);
                        comando = calculaDireccion();
                        realizarMovimiento(comando);
                    } else {
                        if (tengoObjetivo) {
                            comando=obtenerComando();
                            realizarMovimiento(comando);

                            if(comando.equals("rescue")){
                                tengoObjetivo = false;
                                notificarInterlocutor();
                            }
                            else if(comando.equals("stop")){
                                tengoObjetivo = false;
                            }
                        }else if( !tengoObjetivo && AlemanesPendientes.size() > 0 ){
                            // ASIGNA EL NUEVO OBJETIVO
                            objetivoActual = AlemanesPendientes.poll();
                            tengoObjetivo = true;
                            // MOVERSE:
                            comando = obtenerComando();
                            realizarMovimiento(comando);
                        }
                        else if(!tengoObjetivo && AlemanesPendientes.size() == 0){
                            
                            //si no hay alemanes en el mapa, vuelvo a la casilla de salida
                            if (torescue == 0){
                                rescatando = false;
                                nextPosX = posInicioX;
                                nextPosY = posInicioY;
                                
                                if (posActualX == nextPosX && posActualY == nextPosY){
                                    pideParar();
                                    online = false;
                                }
                                else{
                                    comando = calculaDireccion();
                                    realizarMovimiento(comando);  
                                }
                            }
                        }
                    }
                    
                }// FIN IF SI ES UNA RESPUESTA DE UN MOV.
                else {
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
            System.out.println("y me quedan " + fuel + " de fuel" );
            
           }// FIN DEL IF RESCATANDO          
           else{        // si ya he rescatado a todos los alemanes, voy a esperar moverme a la casilla desde la que parti
               String sender = inbox.getSender().name;
                              
               if (sender.equals("Elnath")){
                this.replyWth = inbox.getReplyWith();
                if( inbox.getPerformativeInt() == ACLMessage.INFORM ){
                    int numero_pasos_bajar = (this.posActualZ - this.consultaAltura(this.posActualX, this.posActualY)) / 5;                    
                    // Hacemos refuel
                    if (fuel-(numero_pasos_bajar*fuelrate) < 15.0) {
                        comando = calculaDireccion();
                        this.refuel(comando, numero_pasos_bajar);
                    }
                    else{
                            if (posActualX == nextPosX && posActualY == nextPosY){
                                pideParar();
                                online = false;
                            }
                            else{
                                comando = calculaDireccion();
                                realizarMovimiento(comando);  
                            }
                    }

                }
                else{
                    JsonObject objeto = Json.parse(inbox.getContent()).asObject();
                    System.out.println("Error al volver a base: " + objeto.get("result").asString());
                    
                }
                 
               }
           }
        } // FIN DEL WHILE ONLINE
    }
    
    
    
    /**
    * Notifica al interlocutor que se ha rescatado un alemán pasandole
    * sus coordenadas
    * 
    * @author Adrian Ruiz Lopez
    */  
    public void notificarInterlocutor(){
        JsonObject objeto = new JsonObject();
        objeto.add("posX", objetivoActual.getKey());
        objeto.add("posY", objetivoActual.getValue());
        String content = objeto.toString();
        mandaMensaje(nombreInterlocutor, ACLMessage.INFORM, content);
    }
    
    
    
    /**
    * Realiza un movieminto y lo notifica al controlador
    * 
    * @param comando Dirección a la que se quiere mover
    * 
    * @author Yang Chen
    */  
    public void realizarMovimiento(String comando){
        JsonObject objeto = new JsonObject();
                      
        objeto.add("command", comando);
        String content = objeto.toString();
        mandaMensaje("Elnath", ACLMessage.REQUEST, content);
        System.out.println(nombreDron+ " : voy a hacer: " + comando);
        
        fuel = fuel - fuelrate;
        actualizaPosicion(comando);
    }
    
    
    
    /**
    * Manda el mensaje de check in al interlocutor y al controlador
    * 
    * @param objeto Objeto Json para hacer checkin
    * 
    * @author Yang Chen
    */  
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
    
   
    
    /**
     * calcula la siguiente posicion a mover
     * 
     * @return pos Siguiente movimiento
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
     * @param angulo Angulo al que moverse
     * 
     * @return direccion Dirección a la que moverse
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
     * @return comando Comando a realizar
     * 
     * @author Yang Chen
     */
    public String obtenerComando(){
         
        //actualizamos la posicion que estamos
        nextPosX = objetivoActual.getKey();
        nextPosY = objetivoActual.getValue();
                 
        //si estamos en la casilla de aleman
        if(posActualX == objetivoActual.getKey() && posActualY == objetivoActual.getValue() && tengoObjetivo){
            //si estamos en el suelo y el aleman no ha sido rescatado, hacemos rescate
            if(posActualZ == consultaAltura(posActualX,posActualY)){
                comando="rescue";   
            }
            
            //si no estamos en el suelo, bajamos
            else{
                comando = "moveDW";
            } 
        }
        //si ya hemos rescatado los alemanes y estamos en la posicion inicial
        else if(posActualX == posInicioX && posActualY == posInicioY && !tengoObjetivo){
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
            comando=calculaDireccion();
        }
        return comando;
    }
         
    
    /**
     * obtener angulo a partir de la posicion de un aleman
     * 
     * @param objetivoX Coordenada x del objetivo
     * @param objetivoY Coordenada y del objetivo
     * 
     * @return angulo Angulo en el que se encuentra el objetivo
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
