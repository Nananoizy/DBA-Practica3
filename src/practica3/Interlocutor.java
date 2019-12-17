package practica3;

import DBA.SuperAgent;
import DBAMap.DBAMap;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.imageio.ImageIO;
import org.codehaus.jettison.json.JSONArray;

/**
 * Clase que define al agente, su comportamiento, sensores y comunicaciones
 * 
 * @author Adrián Ruiz López
 * @author David Infante Casas
 * @author Mariana Orihuela Cazorla
 * @author Yang Chen
 */
public class Interlocutor extends SuperAgent {
    /**
     * Nombres de los drones:
     */
     String nombreInterlocutor = "Grupoe";
     String nombreHalcon = "Grupoe_halcon";
     String nombreMosca = "Grupoe_mosca";
     String nombreRescate1 = "Grupoe_rescate1";
     String nombreRescate2 = "Grupoe_rescate2";
    
    /**
     * Drones de la práctica.
     */
    Halcon halcon;
    Mosca mosca;
    Rescate rescate1;
    Rescate rescate2;
    
    /**
     * Estado actual del agente.
     */
    Estados estado;
    
    /**
     * Mapa que recorre el agente.
     */
    DBAMap mapaActual = new DBAMap();
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
     * Bandeja de entrada y salida de mensajes.
     */
    ACLMessage outbox = null;
    ACLMessage inbox = null;
    
    /**
     * Array que contiene las parejas de puntos en las que van a aparecer los drones.
     * 
     * La posicion par va asociada a la x y la impar a la y
     * De esta forma 0 sería la x de la mosca y 1 la y
     * El orden es mosca - halcon - rescate1 - rescate2
     */
    ArrayList<Integer> spawns; 
    
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
     * Posiciones siguientes a las que tiene que ir cada dron.
    */
    Pair<Integer,Integer> siguientePosicionHalcon;
    Pair<Integer,Integer> siguientePosicionMosca;
    
    boolean online=true;
   
    ArrayList<Pair<Integer,Integer>> alemanesTotalesDetectados = new ArrayList<Pair<Integer,Integer>> ();
    ArrayList<Pair<Integer,Integer>> ArrayRescate1 = new ArrayList<Pair<Integer,Integer>> ();
   
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @param mapa Mapa que va a recorrer el agente
     * @throws Exception
     * 
     * @author Adrián Ruiz Lopez
     */
    public Interlocutor(AgentID aid, String mapa, boolean host) throws Exception {
        super(aid);
        this.hosting = host;
        nombreMapaActual = mapa;
        spawns = new ArrayList<Integer>();
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
 
        // REALIZAMOS EL LOGIN EN EL MUNDO
        login();
        // RECIBIMOS MENSAJE DE CONFIRMACIÓN DE LOGIN
        recibeMensaje();
        
        // SI HE CONSEGUIDO SUSCRIBIRME A UN MUNDO
        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
                        
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();  
            cId = inbox.getConversationId();
            sessionKey = objeto.get("session").asString();
            
            ///EXTRAER MAPA
            try {         
                extraerTraza();
            } catch (IOException ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // OBTENEMOS DIMENSIONES DEL MAPA
            dimX = objeto.get("dimx").asInt();
            dimY = objeto.get("dimy").asInt();
            
            
            //INICIALIZAMOS LAS POSICIONES A LAS QUE VAN A TENER QUE IR LOS DRONES
            siguientePosicionHalcon = new Pair(dimX - 49, 49);
            siguientePosicionMosca = new Pair(dimX - 1, 9);
            
            // UNA VEZ OBTENIDOS LOS DATOS, CALCULAMOS POSICIONES
            // Y LEVANTAMOS LOS DRONES:
            try {
                calculaSpawn();
                levantarDrones();                
            } catch (Exception ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // UNA VEZ LEVANTAMOS LOS DRONES, PODEMOS INICIAR EL PROCESO DE RESCATE:
            while ( online ){
                recibeMensaje();
                
                //Obtenemos quien es el SENDER:
                String sender = inbox.getSender().name;
                
                if( sender.equals(nombreHalcon)  ){
                    //System.out.println("INTERLOCUTOR: he recibo un mensaje del HALCON");
                    if (inbox.getPerformativeInt() == ACLMessage.QUERY_REF){ // informando de que necesita un objetivo
                        respondeDireccion(nombreHalcon);
                    }else if( inbox.getPerformativeInt() == ACLMessage.INFORM ){ // informando de sus percepciones
                        recibirInformacion();
                    }
                }else if ( sender.equals(nombreMosca) ){
                    //System.out.println("INTERLOCUTOR: he recibo un mensaje de MOSCA");
                    if (inbox.getPerformativeInt() == ACLMessage.QUERY_REF){ // informando de que necesita un objetivo
                        respondeDireccion(nombreMosca);
                    }else if( inbox.getPerformativeInt() == ACLMessage.INFORM ){ // informando de sus percepciones
                        recibirInformacion();
                    }
                }else if( sender.equals(nombreRescate1)){
                    //System.out.println("INTERLOCUTOR: he recibo un mensaje de RESQ1");
                    // LE PUEDE LLEGAR EL OK DE QUE HA OBTENIDO EL NUEVO OBJETIVO
                    // LE PUEDE LLEGAR LAS PERCEPCIONES DEL RESCATE
                }else if( sender.equals(nombreRescate2)){
                    //System.out.println("INTERLOCUTOR: he recibo un mensaje de RESQ2");
                    // LE PUEDE LLEGAR EL OK DE QUE HA OBTENIDO EL NUEVO OBJETIVO
                    // LE PUEDE LLEGAR LAS PERCEPCIONES DEL RESCATE

                }
                
                

                
            } // Fin bucle while de MODO RESCATE
            
            cancelarPartida();
            
        }
        else{
            System.out.println("\nNo se ha podido hacer login con éxito");
        }
    }
    
    /**
     * Recibir coordenadas de los alemanes enviados por drones y guardarlo en el array
     * 
     * @author Yang Chen
     * @author Adrian Ruiz Lopez
     */
   public void recibirInformacion(){
       JsonObject objeto = Json.parse(inbox.getContent()).asObject();
       JsonArray alemanes = objeto.get("alemanes").asArray();
       
        if( alemanes.size() > 0){
            for( int i=0; i<alemanes.size(); i++){
                JsonObject aleman = alemanes.get(i).asObject();
                int posx= aleman.get("alemanX").asInt();
                int posy= aleman.get("alemanY").asInt();
                Pair<Integer,Integer> coordenada = new Pair(posx,posy);
                // si este aleman no habia sido informado por ningun otro:
                if( !alemanesTotalesDetectados.contains(coordenada) ){
                    alemanesTotalesDetectados.add(coordenada);
                    // decidimos a que cola de rescate meterlo:  (POR AHORA TODOS AL MISMO!)
                    ArrayRescate1.add(coordenada);
                    String content = aleman.toString();
                    mandaMensaje(nombreRescate1, ACLMessage.INFORM,content);
                }
                               
            }
        }

        // PODRIAMOS SEGUIR ANALIZANDO LA INFO OBTENIDA:
        
        for(int i=0;i<alemanesTotalesDetectados.size();i++){
                 //System.out.println("aleman " + i + " se encuentra en la coordenada: x = " + coordenadaAleman.get(i).getKey() + " , y = " + coordenadaAleman.get(i).getValue());
        }
        

        
    }
   

    
    /**
     * Método que levanta a los drones
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void levantarDrones() throws Exception{
        
        //System.out.println("\nLista de posiciones en las que se va a aparecer" + spawns);
        //Creamos los demás drones y les mandamos los datos necesarios para que empiecen a operar
        mosca = new Mosca(new AgentID(nombreMosca), true, nombreMapaActual + ".png");
        halcon = new Halcon(new AgentID(nombreHalcon), true, nombreMapaActual + ".png");
        rescate1 = new Rescate(new AgentID(nombreRescate1), true, nombreMapaActual + ".png");
        rescate2 = new Rescate(new AgentID(nombreRescate2), true, nombreMapaActual + ".png");

        
        // ELEMENTOS DE LA CONEXION
        
        JsonObject objetoJSONInicio = new JsonObject();
        objetoJSONInicio.add("session", sessionKey);
        JsonArray mapaEnviado = new JsonArray();

        objetoJSONInicio.add("posInicioX",spawns.get(0));
        objetoJSONInicio.add("posInicioY",spawns.get(1));
        
        objetoJSONInicio.add("dimMaxX",dimX);
        objetoJSONInicio.add("dimMaxY",dimY);
            
        String content = objetoJSONInicio.toString();
        
        // LEVANTAMOS PRIMERO LA MOSCA
        
        mosca.start();
        
        mandaMensaje(nombreMosca, ACLMessage.INFORM, content);

        recibeMensaje();

        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado la mosca"); 
        }
        else{
            System.out.println("\nNo se ha levantado la mosca");
            cancelarPartida();
        }
        
        // DESPUES EL HALCON
        
        halcon.start();
        
        objetoJSONInicio.remove("posInicioX");
        objetoJSONInicio.remove("posInicioY");
        objetoJSONInicio.add("posInicioX",spawns.get(2));
        objetoJSONInicio.add("posInicioY",spawns.get(3));
            
        content = objetoJSONInicio.toString();    
        
        mandaMensaje(nombreHalcon, ACLMessage.INFORM, content);

        
        recibeMensaje();
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado el halcon"); 
        }
        else{
            System.out.println("\nNo se ha levantado el halcon");
            cancelarPartida();
        }
        
        // Y LOS DOS DRONES DE RESCATE
        
        rescate1.start();
        
        objetoJSONInicio.remove("posInicioX");
        objetoJSONInicio.remove("posInicioY");
        objetoJSONInicio.add("posInicioX",spawns.get(4));
        objetoJSONInicio.add("posInicioY",spawns.get(5));
            
        content = objetoJSONInicio.toString(); 
        
        mandaMensaje(nombreRescate1, ACLMessage.INFORM, content);

        
        recibeMensaje();
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado el rescate 1"); 
        }
        else{
            System.out.println("\nNo se ha levantado el rescate 1");
            cancelarPartida();
        }
        
        rescate2.start();
        
        objetoJSONInicio.remove("posInicioX");
        objetoJSONInicio.remove("posInicioY");
        objetoJSONInicio.add("posInicioX",spawns.get(6));
        objetoJSONInicio.add("posInicioY",spawns.get(7));
            
        content = objetoJSONInicio.toString(); 
        
        mandaMensaje(nombreRescate2, ACLMessage.INFORM, content);

        
        recibeMensaje();
        
        if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
            System.out.println("\nSe ha levantado el rescate 2"); 
        }
        else{
            System.out.println("\nNo se ha levantado el rescate 2");
            cancelarPartida();
        }
        
        int checked = 0;
        //Se comprueba que los drones han podido hacer checkin correctamente
        for (int i = 0; i < 4; i++){
            recibeMensaje();  
            if (inbox.getPerformativeInt() == ACLMessage.CONFIRM){
                //System.out.println ("Se ha podido hacer checkin de: " + inbox.getContent());
                checked++;
            }
            else{
                System.out.println ("No se ha podido hacer checkin de: " + inbox.getContent() + ", cancelando partida");
                cancelarPartida();
            }
        }
        
        if (checked == 4){
            System.out.println ("Todos los drones operativos");
            mandaMensaje(nombreMosca, ACLMessage.CONFIRM, "");
            mandaMensaje(nombreHalcon, ACLMessage.CONFIRM, "");
            mandaMensaje(nombreRescate1, ACLMessage.CONFIRM, "");
            mandaMensaje(nombreRescate2, ACLMessage.CONFIRM, "");

        }
       
        
    }
    
    /**
     * Método que calcula las posiciones donde va a spawnear cada dron
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public boolean calculaSpawn(){
        
        /// Sabiendo que la fly tiene una visibilidad de 20, habrá que encajarla en la esquina
        // superior izquierda dejando 10 unidades de margen con los bordes
        //System.out.println("\n Entro en calcula spawn");
        int xTemp, yTemp;
        boolean posicionCorrecta = false;
        
        //MOSCA
        
        xTemp = 9;
        yTemp = 9;
        
        posicionCorrecta = compruebaCasilla(xTemp,yTemp,255);
        
        //Mientras que la posicion no sea correcta y no se haya llegado a los limites del mundo
        while (!(posicionCorrecta) && (xTemp > 0) && (yTemp > 0)){
            xTemp --;
            yTemp --;
            posicionCorrecta = compruebaCasilla(xTemp,yTemp,255);
        }
        
        
        //en caso de que compruebaCasilla devuelva false definitivo, se hace un cancel
        
        if (!compruebaCasilla(xTemp,yTemp,255)){
            cancelarPartida();
            return false;
        }
        else{  // si la posicion es correcta, añadimos al array las posiciones
            spawns.add(xTemp);
            spawns.add(yTemp);
            
            //System.out.println("\n Puedo spawnear mosca en: " + xTemp + " " + yTemp);

        }
        
        /// HALCON
        
        xTemp = 49;
        yTemp = dimY - 49;
        
        posicionCorrecta = compruebaCasilla(xTemp,yTemp,230);
        
        //Mientras que la posicion no sea correcta y no se haya llegado a los limites del mundo
        while (!(posicionCorrecta) && (xTemp > 0) && (yTemp < dimY)){
            xTemp --;
            yTemp ++;
            posicionCorrecta = compruebaCasilla(xTemp,yTemp,230);
        }
        
        //en caso de que compruebaCasilla devuelva false definitivo, se hace un cancel
        
        if (!compruebaCasilla(xTemp,yTemp,230)){
            cancelarPartida();
            return false;
        }
        else{  // si la posicion es correcta, añadimos al array las posiciones
            spawns.add(xTemp);
            spawns.add(yTemp);
            //System.out.println("\n Puedo spawnear halcon en: " + xTemp + " " + yTemp);
        }
        
        /// RESCATE 1
        
        xTemp = dimX / 2;
        yTemp = dimY / 4;
        
        posicionCorrecta = compruebaCasilla(xTemp,yTemp,255);
        
        //Mientras que la posicion no sea correcta y no se haya llegado a los limites del mundo
        while (!(posicionCorrecta) && (xTemp < dimX)){
            xTemp ++;
            posicionCorrecta = compruebaCasilla(xTemp,yTemp,255);
        }
        
        //en caso de que compruebaCasilla devuelva false definitivo, se hace un cancel
        
        if (!compruebaCasilla(xTemp,yTemp,255)){
            cancelarPartida();
            return false;
        }
        else{  // si la posicion es correcta, añadimos al array las posiciones
            spawns.add(xTemp);
            spawns.add(yTemp);
            //System.out.println("\n Puedo spawnear rescate1 en: " + xTemp + " " + yTemp);
        }
        
        /// RESCATE 2
        
        xTemp = dimX / 2;
        yTemp = dimY - (dimY / 4);
        
        posicionCorrecta = compruebaCasilla(xTemp,yTemp,255);
        
        //Mientras que la posicion no sea correcta y no se haya llegado a los limites del mundo
        while (!(posicionCorrecta) && (xTemp < dimX)){
            xTemp ++;
            posicionCorrecta = compruebaCasilla(xTemp,yTemp,255);
        }
        
        //en caso de que compruebaCasilla devuelva false definitivo, se hace un cancel
        
        if (!compruebaCasilla(xTemp,yTemp,255)){
            cancelarPartida();
            return false;
        }
        else{  // si la posicion es correcta, añadimos al array las posiciones
            spawns.add(xTemp);
            spawns.add(yTemp);
            //System.out.println("\n Puedo spawnear rescate2 en: " + xTemp + " " + yTemp);

        }
        
        
        return true;
    }
    
    /**
     * Método que comprueba unas casillas concretas
     * 
     * @param x Coordenada x que se va a comprobar
     * @param y Coordenada y que se va a comprobar
     * @param alturaMax altura maxima a la que puede volar el dron concreto
     * 
     * @author Mariana Orihuela Cazorla
     * @author David Infante Casas
     */
    
    public boolean compruebaCasilla(int x, int y, int alturaMax){
        
        /// Hay que comprobar tanto si van a spawnear drones en esa posicion como si
        // no se puede llegar porque es la altura maxima
        
        int alturaCasilla;
        
        boolean sePuedeSpawnear = true;
        
        // Suponemos que x, y, dimX y dimY empiezan en 0
       // alturaCasilla = consultaAltura(x, y);
        alturaCasilla = mapaActual.getLevel(x, y);
        
        if (alturaCasilla > alturaMax){
            sePuedeSpawnear = false;
        }
        
        //recorro el vector de spawns y compruebo que no se vaya a instanciar otro dron ahi
        if (spawns.size() > 0){
            
            for (int i = 0; i < spawns.size(); i++){
                
                // si el i es par compruebo la x y si no coincide, ya no estan en la misma casilla
                if ((i % 2) == 0){
                    
                    if (x == spawns.get(i)){
                        
                        // compruebo la y y si es igual, termino
                        if (y == spawns.get(i+1)){
                            sePuedeSpawnear = false;
                        }
                    }
                }
            }
        }
        
        return sePuedeSpawnear;
    }
    
    /**
     * Método que crea un mensaje que se manda
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public void mandaMensaje(String receptor, int performativa, String content){
        
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
    
    public void recibeMensaje(){
        
        try {
                inbox = receiveACLMessage();
            } catch (InterruptedException ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("No se puede recibir el mensaje");
            }
        
    }
    
    /**
     * Envío del mensaje para hacer login
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void login() {
        /* Preparación del Mensaje */
        JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("map",nombreMapaActual);
        objetoJSON.add("user", "Eagle");
        objetoJSON.add("password", "Hzrwtags");
        
        String mensaje = objetoJSON.toString();
        
        /* envío */
        outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Elnath"));
        outbox.setPerformative(ACLMessage.SUBSCRIBE);
        outbox.setContent(mensaje);
        this.send(outbox);
        
    }
    
    /**
     * Envío del mensaje para cancelar la partida
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void cancelarPartida(){
        
        outbox = new ACLMessage();
            outbox.setSender(this.getAid());
            outbox.setReceiver(new AgentID("Elnath"));
            outbox.setPerformative(ACLMessage.CANCEL);
            outbox.setConversationId(cId);
            outbox.setContent("");
            this.send(outbox);
            
            recibeMensaje();
            
        if(inbox.getPerformativeInt() == ACLMessage.AGREE)
            System.out.println("\nSe ha cerrado sesión");
        else
            System.out.println("\nNo se ha cerrado sesión"); 
            
    }
    
    /**
     * Extracción de la traza del mapa
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    public void extraerTraza() throws FileNotFoundException, IOException {
        JsonObject injson = Json.parse(inbox.getContent()).asObject();
        JsonArray img = injson.get("map").asArray();
        mapaActual.fromJson(img);
        System.out.println("Saving file ./maps/"+ nombreMapaActual +".png");
        mapaActual.save("./"+nombreMapaActual+".png"); 
    }
    
    
    /**
     * Método que nos devuelve la dirección en la que tiene que ir el dron que pregunte
     * 
     * @author Mariana Orihuela Cazorla
     * @param x: posicion actual x del dron
     * @param y: posicion actual y del dron
     * @param nombreDron: nombre del dron que pide dirección
     */
    public void respondeDireccion(String nombreDron){
        JsonObject objeto = Json.parse(inbox.getContent()).asObject();
        int x = objeto.get("posX").asInt();
        int y = objeto.get("posY").asInt();
        
        // calcula dependiendo de dónde están los demás drones, a dónde tiene que ir el dron actual
        int irAX = -1;
        int irAY = -1;
        JsonObject objetoJSON = new JsonObject();
                
        if (nombreDron.equals( nombreHalcon )){
            irAX = siguientePosicionHalcon.getKey();
            irAY = siguientePosicionHalcon.getValue();
            
            ///si he llegado a la esquina superior derecha, bajo abajo del todo
            if ((x == irAX && y == irAY) && (x == (dimX - 49) && y == 49)){

                siguientePosicionHalcon = new Pair(dimX - 49, dimY - 49);
                
                irAX = siguientePosicionHalcon.getKey();
                irAY = siguientePosicionHalcon.getValue();
                
                //System.out.println("Siguiente posicion del halcon: " + irAX + " , " + irAY);
   
            }
            //si ya he llegado a la esquina de abajo, vuelvo a base
            else if((x == irAX && y == irAY) && (x == (dimX - 49) && y == dimY - 49)){
                
                siguientePosicionHalcon = new Pair(spawns.get(2), spawns.get(3));
                
                irAX = siguientePosicionHalcon.getKey();
                irAY = siguientePosicionHalcon.getValue();
                
            }
            
            //si ya estoy en base, paro
            else if((x == irAX && y == irAY) && (x == spawns.get(2) && y == spawns.get(3))){
                
                siguientePosicionHalcon = new Pair(-1,-1);
                
                irAX = siguientePosicionHalcon.getKey();
                irAY = siguientePosicionHalcon.getValue();
                
            }
            
            
            objetoJSON.add("irAX",irAX);
            objetoJSON.add("irAY",irAY);   
            String mensaje = objetoJSON.toString();
            
            //System.out.println("Respondemos a halcon");
            mandaMensaje(nombreHalcon, ACLMessage.INFORM, mensaje);

        }
        
        if (nombreDron.equals( nombreMosca )){
            
            irAX = siguientePosicionMosca.getKey();
            irAY = siguientePosicionMosca.getValue();
            
            ///para que no se quede pidiendo en el sitio y se mueva a un sitio aleatorio
            
            //SI NO ESTA TOCANDO NINGUNA ARISTA SE MOVERIA NW HASTA LA SIGUIENTE PARED
            if ((x == irAX && y == irAY)){
                
                if ((x == dimX - 1)){
                    siguientePosicionMosca = new Pair(0, y + 9);
                    irAX = siguientePosicionMosca.getKey();
                    irAY = siguientePosicionMosca.getValue();
                }
                else if((x == 0)){
                    siguientePosicionMosca = new Pair(dimX - 1, y + 9);
                    irAX = siguientePosicionMosca.getKey();
                    irAY = siguientePosicionMosca.getValue();
                }
                else{
                    siguientePosicionMosca = new Pair(dimX - 1, y);
                    irAX = siguientePosicionMosca.getKey();
                    irAY = siguientePosicionMosca.getValue();
                }
                
                System.out.println("Siguiente posicion de la mosca: " + irAX + " , " + irAY);
                
            }
            
            objetoJSON.add("irAX",irAX);
            objetoJSON.add("irAY",irAY);   
            String mensaje = objetoJSON.toString();
            
            //System.out.println("Respondemos a halcon");
            mandaMensaje(nombreMosca, ACLMessage.INFORM, mensaje);

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
