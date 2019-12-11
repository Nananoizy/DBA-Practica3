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
     * Drones de la práctica.
     */
    
    Halcon halcon;
    Mosca mosca;
    Rescate rescate1, rescate2;
    /**
     * Estado actual del agente.
     */
    Estados estado;
    /**
     * Mapa que recorre el agente.
     */
    BufferedImage mapaActual;   
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
   
    ArrayList<Pair<Integer,Integer>> coordenadaAleman = new ArrayList<Pair<Integer,Integer>> ();
   
    
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
 
        //bandejas de entrada y salida

        
        //if hosteo..
        //intentamos suscribirnos al mundo
        login();
        
        recibeMensaje();
        
        // SI HE CONSEGUIDO SUSCRIBIRME A UN MUNDO
        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
            //System.out.println("\nSe ha podido hacer login con éxito");
            
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();  
            cId = inbox.getConversationId();
            sessionKey = objeto.get("session").asString();
            
            try {         
                ///EXTRAER MAPA
                extraerTraza();
            } catch (IOException ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //DIMENSIONES DEL MAPA
            dimX = objeto.get("dimx").asInt();
            dimY = objeto.get("dimy").asInt();
            
            
            //System.out.println("dimx: " + dimX + " dimy: " + dimY);
            
            //INICIALIZAMOS LAS POSICIONES A LAS QUE VAN A TENER QUE IR LOS DRONES
            siguientePosicionHalcon = new Pair(dimX - 49, 49);
            siguientePosicionMosca = new Pair(dimX - 1, 9);
            
            try {   
                // Una vez hemos conseguido los datos que nos interesan, informamos a los distintos drones

                calculaSpawn();
                levantarDrones();
                
                
            } catch (Exception ex) {
                Logger.getLogger(Interlocutor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // BUCLE CORRESPONDIENE A LA PARTE DE RESCATE:
            while ( online ){
                recibeMensaje();
                //AgentID receptor = inbox.getReceiver();
                
                if (inbox.getPerformativeInt() == ACLMessage.QUERY_REF){
                    
                    int veaX, veaY;
                    String nombreDron;
                    objeto = Json.parse(inbox.getContent()).asObject();
                    nombreDron = objeto.get("dron").asString();
                    veaX = objeto.get("posX").asInt();
                    veaY = objeto.get("posY").asInt();

                    respondeDireccion(veaX, veaY, nombreDron);
                    //online = false;
                }
                else if (inbox.getPerformativeInt() == ACLMessage.REQUEST){
                    
                    objeto = Json.parse(inbox.getContent()).asObject();
                    recibirCoordenadas(objeto);
                    
                    mandaMensaje("Grupoe__halcon",ACLMessage.CONFIRM,"");
                    
                    //online = false;
                }
                
                //deberiamos discintiguir quien le estan enviando el mensaje e ir realizando las diferentes acciones.
                
            }
            
            cancelarPartida();
            
        }
        else{
            System.out.println("\nNo se ha podido hacer login con éxito");
        }
    }
    
    
   public void recibirCoordenadas(JsonObject objeto){
        
       // JsonObject objeto = Json.parse(entrada.getContent()).asObject();
        
       // if(entrada.getPerformativeInt()==ACLMessage.REQUEST){
            int posx=objeto.get("posicionx").asInt();
            int posy=objeto.get("posiciony").asInt();
            
            Pair<Integer,Integer> coordenada = new Pair(posx,posy);
            coordenadaAleman.add(coordenada);
            //System.out.println("aniadido posicion de aleman");
        
        
         for(int i=0;i<coordenadaAleman.size();i++){
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
        mosca = new Mosca(new AgentID("Grupoe__mosca"), true, nombreMapaActual+".png");
        halcon = new Halcon(new AgentID("Grupoe__halcon"), true, nombreMapaActual+".png");
        rescate1 = new Rescate(new AgentID("Grupoe__rescate1"), true, nombreMapaActual+".png");
        rescate2 = new Rescate(new AgentID("Grupoe__rescate2"), true, nombreMapaActual+".png");
        
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
        
        mandaMensaje("Grupoe__mosca", ACLMessage.INFORM, content);
        
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
        
        mandaMensaje("Grupoe__halcon", ACLMessage.INFORM, content);
        
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
        
        mandaMensaje("Grupoe__rescate1", ACLMessage.INFORM, content);
        
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
        
        mandaMensaje("Grupoe__rescate2", ACLMessage.INFORM, content);
        
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
            mandaMensaje("Grupoe__mosca", ACLMessage.CONFIRM, "");
            mandaMensaje("Grupoe__halcon", ACLMessage.CONFIRM, "");
            mandaMensaje("Grupoe__rescate1", ACLMessage.CONFIRM, "");
            mandaMensaje("Grupoe__rescate2", ACLMessage.CONFIRM, "");
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
        alturaCasilla = consultaAltura(x, y);
        
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
            JsonArray ja = injson.get("map").asArray();
            byte data[] = new byte [ja.size()];
            for (int i = 0; i < data.length; i++){
                data[i] = (byte) ja.get(i).asInt();
            }
            FileOutputStream fos = new FileOutputStream(nombreMapaActual+".png");
            fos.write(data);
            fos.close();
            System.out.println("Traza guardada");
            
            mapaActual = ImageIO.read(new File(nombreMapaActual+".png")); /// mapaActual YA es la matriz
    }
    
    
    /**
     * Método que nos devuelve la dirección en la que tiene que ir el dron que pregunte
     * 
     * @author Mariana Orihuela Cazorla
     * @param x: posicion actual x del dron
     * @param y: posicion actual y del dron
     * @param nombreDron: nombre del dron que pide dirección
     */
    public void respondeDireccion(int x, int y, String nombreDron){
        // calcula dependiendo de dónde están los demás drones, a dónde tiene que ir el dron actual
        
        int irAX = -1;
        int irAY = -1;
        JsonObject objetoJSON = new JsonObject();
        
        System.out.println(nombreDron);
        
        if (nombreDron.equals( "halcon" )){
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
            else if((x == irAX && y == irAY) && (x == (dimX - 49) && y == 49)){
                
                siguientePosicionHalcon = new Pair(49, dimY - 49);
                
                irAX = siguientePosicionHalcon.getKey();
                irAY = siguientePosicionHalcon.getValue();
                
            }
            
            objetoJSON.add("irAX",irAX);
            objetoJSON.add("irAY",irAY);   
            String mensaje = objetoJSON.toString();
            
            //System.out.println("Respondemos a halcon");
            mandaMensaje("Grupoe__halcon", ACLMessage.INFORM, mensaje);
        }
        
        if (nombreDron.equals( "mosca" )){
            
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
            mandaMensaje("Grupoe__mosca", ACLMessage.INFORM, mensaje);
        }
        
    }
    
    /**
     * Método que nos devuelve la altura de una determinada casilla del mapa
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public int consultaAltura(int x, int y){
        int altura = new Color(mapaActual.getRGB(x, y)).getBlue();
        return altura;
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
