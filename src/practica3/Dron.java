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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
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
     * Nombre del interlocutor.
     */
    String nombreInterlocutor = "Grupoe";
    
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
    //DBAMap mapa;
    BufferedImage mapa;
    
    /**
     * Nombre del dron.
     */
    String nombreDron;
    
    /**
     * variable que indica que esta en funcionamiento.
     */
    boolean online = false;
    
    /**
     * Posicion a la que tiene que ir.
     */
    int posActualX,posActualY,posActualZ;
    
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
    float fuel;
    boolean goal;
    String status; // Y ESTE STATUS?
    int torescue; // ni idea de que es tamapoco
    double energy; // ni idea
    boolean cancel; // ni idea
    
    
     ArrayList<Pair<Integer,Integer>> coordAleman = new ArrayList<Pair<Integer,Integer>> ();
    
    
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
        this.nextPosX = -1;
        this.nextPosY = -1;
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
     * Método que ejecuta el dron para preguntar acerca de la siguiente posición a la que moverse
     * 
     * 
     * @author Mariana Orihuela Cazorla
     */
    
    public void pedirSiguientePosicion() {
        JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("posX", posActualX);
        objetoJSON.add("posY", posActualY);
        String content = objetoJSON.toString();            
        mandaMensaje(nombreInterlocutor, ACLMessage.QUERY_REF, content);
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
       // mapa.load(urlArchivo+".png");
        
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
    * Método que manda un mensaje al controlador para parar a un dron
    * 
    * 
    * @author Mariana Orihuela Cazorla
    */
    public void pideParar(){
      JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("command", "stop");
        String content = objetoJSON.toString();            
        mandaMensaje("Elnath", ACLMessage.REQUEST, content);
    }
    
    /**
    * Método que consulta si el dron actual va a colisionar en la casilla objetivo
    * 
    * 
    * @author Mariana Orihuela Cazorla
    */
    public int[] casillaFuturaDron(String otrodrondireccion, int x, int y, int z){
        
        switch(otrodrondireccion){
            case "moveUP":
                z = z + 1;
            break;
            
            case "moveDW":
                z = z-1;
            break;
            
            case "moveN":
                y = y -1;
            break;
            
            case "moveNW":
                y = y -1;
                x = x - 1;
            break;
            
            case "moveW":
                x = x -1;
            break;
            
            case "moveSW":
                y = y +1;
                x = x -1;
            break;
            
            case "moveS":
                y = y +1;
            break;
            
            case "moveSE":
                y = y +1;
                x = x +1;
            break;
            
            case "moveE":
                x = x +1;
            break;
            
            case "moveNE":
                y = y -1;
                x = x +1;
            break;
            
                
        }
        
        int[] posicionesDron = {x, y, z};
        
        return posicionesDron;
        
    }
    
    
    /**
    * Método que consulta si el dron actual va a colisionar en la casilla objetivo
    * 
    * 
    * @author Mariana Orihuela Cazorla
    */
    public String compruebaAwacs(String direccion){
        
        
        for (int i = 0; i < awacs.size(); i++){
            
            JsonObject dron = awacs.get(i).asObject();
            
            int otroposx = dron.get("x").asInt();
            int otroposy = dron.get("y").asInt();
            int otroposz = dron.get("z").asInt();
            String otrodirection = dron.get("direction").asString();
            int [] posicionFuturaDron;
            
            //Si voy a moverme en una direccion concreta compruebo si hay drones ahi o si los va a haber
            
            switch(direccion){
                //SI ME VOY A MOVER HACIA ARRIBA, TENGO QUE COMPROBAR QUE NO HAYA YA ENCIMA MIO O QUE NO VAYAN A MOVERSE A ENCIMA MIO
                case "moveUP":
                    
                    // SI ESTÁ EN NUESTRA MISMA CASILLA
                    if (otroposx == posActualX && otroposy == posActualY){
                        
                        //Si está por encima nuestra justo una casilla o dos
                        if (otroposz == posActualZ + 1 || otroposz == posActualZ + 2){
                            //si no tiene planeado moverse o si quiere bajar, nos apartamos a un lado
                            if((otrodirection.equals("") || otrodirection.equals("moveDW")) && otroposz == posActualZ + 1){
                                nextPosY = posActualY + 1;
                            }
                            else{  //dejamos que pase por encima nuestra
                                direccion = "";
                            }
                        }

                    }
                    // SI NO ESTÁ EN NUESTRA MISMA CASILLA
                    else{
                        // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                        
                        posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);
                        
                        //si voy a colisionar cuando me mueva, me quedo en el sitio
                        if (posicionFuturaDron[0] == posActualX && posicionFuturaDron[1] == posActualY && posicionFuturaDron[2] == posActualZ + 1){
                            direccion = "";
                        }
                        
                    }
                break;
                
                //SI ME VOY A MOVER HACIA ABAJO, TENGO QUE COMPROBAR QUE NO HAYA YA DEBAJO MIO O QUE NO VAYAN A MOVERSE A DEBAJO MIO
                case "moveDW":
                    
                    // SI ESTÁ EN NUESTRA MISMA CASILLA
                    if (otroposx == posActualX && otroposy == posActualY){
                        
                        //Si está por encima nuestra justo una casilla o dos
                        if (otroposz == posActualZ - 1 || otroposz == posActualZ - 2){
                            //si no tiene planeado moverse o si quiere bajar, nos apartamos a un lado
                            if((otrodirection.equals("") || otrodirection.equals("moveUP")) && otroposz == posActualZ - 1){
                                //doy nueva posicion ??? o simplemente cambio direccion ???
                                nextPosY = posActualY + 1;
                            }
                            else{  //dejamos que pase por encima nuestra
                                direccion = "";
                            }
                        }

                    }
                    // SI NO ESTÁ EN NUESTRA MISMA CASILLA
                    else{
                        // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                        
                        posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);
                        
                        //si voy a colisionar cuando me mueva, me quedo en el sitio
                        if (posicionFuturaDron[0] == posActualX && posicionFuturaDron[1] == posActualY && posicionFuturaDron[2] == posActualZ - 1){
                            direccion = "";
                        }
                        
                    }
                break;
                
                //SI ME VOY A MOVER HACIA EL NORTE
                case "moveN":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    //si voy a colisionar cuando me mueva, me quedo en el sitio
                    if (posicionFuturaDron[0] == posActualX && posicionFuturaDron[1] == posActualY - 1 && posicionFuturaDron[2] == posActualZ){
                        
                        //si el otro dron no se va a mover y voy a colisionar, lo sobrepaso
                        //OJO!!!!! CAMBIAR PARA QUE NO SUPERE LIMITES DEL ALTURA MAXIMA, CAMBIAR POR MANO DERECHA
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
                //SI ME VOY A MOVER HACIA EL NOROESTE
                case "moveNW":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    //si voy a colisionar cuando me mueva, me quedo en el sitio
                    if (posicionFuturaDron[0] == posActualX -1 && posicionFuturaDron[1] == posActualY - 1 && posicionFuturaDron[2] == posActualZ){
                        
                        //si el otro dron no se va a mover y voy a colisionar, lo sobrepaso
                        //OJO!!!!! CAMBIAR PARA QUE NO SUPERE LIMITES DEL ALTURA MAXIMA, CAMBIAR POR MANO DERECHA
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
                //SI ME VOY A MOVER HACIA EL NOROESTE
                case "moveW":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    //si voy a colisionar cuando me mueva, me quedo en el sitio
                    if (posicionFuturaDron[0] == posActualX -1 && posicionFuturaDron[1] == posActualY && posicionFuturaDron[2] == posActualZ){
                        
                        //si el otro dron no se va a mover y voy a colisionar, lo sobrepaso
                        //OJO!!!!! CAMBIAR PARA QUE NO SUPERE LIMITES DEL ALTURA MAXIMA, CAMBIAR POR MANO DERECHA
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
                //SI ME VOY A MOVER HACIA EL SUROESTE
                case "moveSW":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    //si voy a colisionar cuando me mueva, me quedo en el sitio
                    if (posicionFuturaDron[0] == posActualX -1 && posicionFuturaDron[1] == posActualY + 1 && posicionFuturaDron[2] == posActualZ){
                        
                        //si el otro dron no se va a mover y voy a colisionar, lo sobrepaso
                        //OJO!!!!! CAMBIAR PARA QUE NO SUPERE LIMITES DEL ALTURA MAXIMA, CAMBIAR POR MANO DERECHA
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
                //SI ME VOY A MOVER HACIA EL NOROESTE
                case "moveS":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    if (posicionFuturaDron[0] == posActualX && posicionFuturaDron[1] == posActualY + 1 && posicionFuturaDron[2] == posActualZ){
 
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
                //SI ME VOY A MOVER HACIA EL SURESTE
                case "moveSE":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    if (posicionFuturaDron[0] == posActualX +1 && posicionFuturaDron[1] == posActualY + 1 && posicionFuturaDron[2] == posActualZ){
 
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
                //SI ME VOY A MOVER HACIA EL este
                case "moveE":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    if (posicionFuturaDron[0] == posActualX +1 && posicionFuturaDron[1] == posActualY && posicionFuturaDron[2] == posActualZ){
 
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
                //SI ME VOY A MOVER HACIA EL NORESTE
                case "moveNE":
                    
                    // tengo que comprobar que no va a haber ningun dron que se vaya a mover a la casilla en la que voy a estar
                    posicionFuturaDron = casillaFuturaDron(otrodirection, otroposx, otroposy, otroposz);

                    if (posicionFuturaDron[0] == posActualX +1 && posicionFuturaDron[1] == posActualY - 1 && posicionFuturaDron[2] == posActualZ){
 
                        if (otrodirection.equals("")){
                            direccion = "moveUP";
                        }
                        else
                            direccion = "";
                    }

                break;
                
            }
            
        }
        
        return direccion;
    }
    
    
    /**
    * Método que actualiza la posicion del dron una vez se ha movido con exito
    * 
    * 
    * @author Mariana Orihuela Cazorla
    */
    public void actualizaPosicion(String direccion){
        
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
    
    
    /**
    * Método que calcula la direccion en la que tiene que ir el dron dado un punto de destino
    * 
    * 
    * @author Mariana Orihuela Cazorla
    */
    public String calculaDireccion(){
        
        String direccion = "";
        
        //Comprobamos si en nextPosX va a haber o hay algun obstaculo y si es asi lo esquivamos por encima
                
        
        ///TIENE QUE COMPROBAR ALTURAS Y REFUEL
        //System.out.println("x: " + posActualX + " , " + nextPosX + " y " + posActualY + " , " + nextPosY);
        // Si la y es mayor y la x es igual, va al Norte
        if (posActualX == nextPosX && posActualY > nextPosY){
            //si tengo un muro delante y no he subido al nivel maximo, asciendo
            if (consultaAltura(posActualX , posActualY - 1) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            
            ///SI NO PUEDEN ASCENDER MAS Y TIENEN UN OBJETO DELANTE, HACER MANO DERECHA...
            else{
                direccion = "moveN";
            }
            
        }
        //Si la x de destino es mayor y la y es menor, va hacia el noreste
        else if (posActualX < nextPosX && posActualY > nextPosY){
            if (consultaAltura(posActualX + 1, posActualY - 1) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            else
                direccion = "moveNE";
        }
        //Si la x de destino es mayor y la y es igual, va hacia el este
        else if (posActualX < nextPosX && posActualY == nextPosY){
            if (consultaAltura(posActualX + 1, posActualY) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            else
                direccion = "moveE";
        }
        //Si la x de destino es mayor y la y es mayor, va hacia el sureste
        else if (posActualX < nextPosX && posActualY < nextPosY){
            if (consultaAltura(posActualX + 1, posActualY + 1) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            else
                direccion = "moveSE";
        }
        //Si la y de destino es menor y la x es igual, va hacia el sur
        else if (posActualX == nextPosX && posActualY < nextPosY){
            if (consultaAltura(posActualX, posActualY + 1) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            else
                direccion = "moveS";
        }
        //Si la x de destino es menor y la y es mayor, va hacia el suroeste
        else if (posActualX > nextPosX && posActualY < nextPosY){
            if (consultaAltura(posActualX - 1, posActualY + 1) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            else
                direccion = "moveSW";
        }
        //Si la x de destino es menor y la y es igual, va hacia el oeste
        else if (posActualX > nextPosX && posActualY == nextPosY){
            if (consultaAltura(posActualX - 1, posActualY) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            else
                direccion = "moveW";
        }
        //Si la x de destino es menor y la y es menor, va hacia el noroeste
        else if (posActualX > nextPosX && posActualY > nextPosY){
            if (consultaAltura(posActualX - 1, posActualY - 1) > posActualZ && maxlevel > posActualZ){
                direccion = "moveUP";
            }
            else
                direccion = "moveNW";
        }
        
        direccion = compruebaAwacs(direccion);
        
        return direccion;
    }
    
    /**
     * Realiza las percepciones con el controlador
     * 
     * @author Adrian Ruiz Lopez
     */
    public void cargarPercepciones(){
        mandaMensaje("Elnath", ACLMessage.QUERY_REF ,"");
        recibeMensaje("mensaje de pedir Percepciones");
        this.replyWth = inbox.getReplyWith();
        if(inbox.getPerformativeInt() == ACLMessage.INFORM ){
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();
            JsonObject result =  objeto.get("result").asObject();
            gps = result.get("gps").asObject();
            infrared = result.get("infrared").asArray();
            gonio = result.get("gonio").asObject();
            fuel = result.get("fuel").asFloat();
            goal = result.get("goal").asBoolean();
            status = result.get("status").asString();
            awacs = result.get("awacs").asArray();
            torescue = result.get("torescue").asInt();
            energy = result.get("energy").asDouble();
            cancel =result.get("cancel").asBoolean();
        }
        
    }
    

    /**
    * Recorre el sensor de inflarojos y decuelve las posiciones de los alemanes
    *
    * @author Adrian Ruiz Lopez
    * @author Yang Chen
    */
    public void obtenerAlemanesInfrarojos(){
        List<JsonValue> lista = infrared.values();       
        List<Integer> posi = new ArrayList<Integer>();
        //System.out.println("N=" + range);
        int anchura = range;
        int radio = anchura/2;
        
        // Obtenemos las posiciones relativas de los alemanes en el infrarojos.
        for( int y=0;y<range;y++){
            for(int x=0;x<range;x++){
                if( lista.get((y*anchura)+x).asInt() == 1 ){
                       posi.add(x);
                       posi.add(y);
                }
            }
        }
        
        // convertimos dichas posiciones relativas en posiciones en el mundo.
        for(int i=0; i<posi.size();i+=2){
            int x = posi.get(i);
            int y = posi.get(i+1);
            
            
            if( x<radio && y<radio ){
                x = gps.get("x").asInt() - Math.abs(x-radio);
                y = gps.get("y").asInt() -  Math.abs(y-radio) ;
            }else if ( x>radio && y<radio){
                x = gps.get("x").asInt() +  Math.abs(x-radio) ;
                y = gps.get("y").asInt() -  Math.abs(y-radio) ;
            }else if ( x<radio && y>radio ){
                x = gps.get("x").asInt() -  Math.abs(x-radio) ;
                y = gps.get("y").asInt() +  Math.abs(y-radio) ;
            }else if ( x>radio && y>radio ){
                x = gps.get("x").asInt() +  Math.abs(x-radio) ;
                y = gps.get("y").asInt() +  Math.abs(y-radio) ;
            }else if ( x==radio ){
                x = gps.get("x").asInt();
                if( y < radio ){
                    y = gps.get("y").asInt() -  Math.abs(y-radio) ;
                }else if ( y > radio ){
                    y = gps.get("y").asInt() +  Math.abs(y-radio) ;
                }else{
                    y = gps.get("y").asInt();
                }
            }else if ( y==radio ){
                y = gps.get("y").asInt();
                if( x < radio ){
                    x = gps.get("x").asInt() -  Math.abs(x-radio) ;
                }else if ( x > radio ){
                    x = gps.get("x").asInt() +  Math.abs(x-radio) ;
                }else{
                    x = gps.get("x").asInt();
                }
            }
            // añadimos las posciones en el mundo al array:
            Pair<Integer,Integer> aleman = new Pair(x,y);
            coordAleman.add(aleman);
            
        }
          

    }// fin obetenerAlemanesInfrarojos
    
    
    
    /**
     * Percibe un alemán con el sensor gonio
     * 
     * @author David Infante Casas
     */
    public void obtenerAlemanGonio() {
        double distancia = gonio.get("distance").asDouble();
        double angulo = gonio.get("angle").asDouble();
        int x_aleman = 0, y_aleman = 0;
        
        System.out.println("distancia: " + distancia + " angulo: "+ angulo);
        
        // Según el ángulo calculamos la posición
        if (angulo >= 0 && angulo <= 90) { // N-E
            x_aleman = (int) (posActualX - Math.sin(Math.toRadians(angulo)) * distancia);
            y_aleman = (int) (posActualY + Math.cos(Math.toRadians(angulo)) * distancia);
        } else if (angulo > 90 && angulo <= 180) { // E-S
            x_aleman = (int) (posActualX + Math.sin(Math.toRadians(angulo)) * distancia);
            y_aleman = (int) (posActualY - Math.cos(Math.toRadians(angulo)) * distancia);
        } else if (angulo > 180 && angulo <= 270) { // S-W
            x_aleman = (int) (posActualX - Math.sin(Math.toRadians(angulo)) * distancia);
            y_aleman = (int) (posActualY + Math.cos(Math.toRadians(angulo)) * distancia);
        } else if (angulo > 270 && angulo <= 360) { // W-N
            x_aleman = (int) (posActualX + Math.sin(Math.toRadians(angulo)) * distancia);
            y_aleman = (int) (posActualY - Math.cos(Math.toRadians(angulo)) * distancia);
        }
        
        Pair<Integer,Integer> aleman = new Pair(x_aleman, y_aleman);
        if (!comprobarAlemanRepetido(aleman)) coordAleman.add(aleman);
        else System.out.println("Alemán repetido detectado con infrarrojos");
    }
    
    /**
     * Comprueba que el alemán pasado como parámetro
     * no esté presente ya en el array
     * 
     * @param aleman Aleman para comprobar si está ya en el array
     * 
     * @return true si ya está, false si no está en el array
     * 
     * @author David Infante Casas
     */
    public boolean comprobarAlemanRepetido(Pair<Integer,Integer> aleman) {
        for (Pair<Integer,Integer> alem : coordAleman) {
            if (alem.getKey() == aleman.getKey() && alem.getValue() == aleman.getValue()) {
                return true;
            }
        }
        return false;
    }
    
    
    
    /**
     * Muestra la lista de alemanes por pantalla

     * @author David Infante Casas
     */
    public void mostrarArrayAlemanes() {
        for (Pair<Integer,Integer> alem : coordAleman) {
            System.out.println(alem.getKey() + ", " + alem.getValue() + "\n");
        }
    }    
    
    
    /**
     * Mandar coordenadas de alemanes al interlocutor
     * 
     * 
     * @author Yang Chen
     * @author Adrian Ruiz Lopez
     */
    public void mandarInformacionPercepciones(){
        JsonObject informacion = new JsonObject();
        JsonArray alemanes = new JsonArray();
        
        
        for(int i=0;i<coordAleman.size();i++){
            int px=coordAleman.get(i).getKey();
            int py=coordAleman.get(i).getValue();
            JsonObject aleman = new JsonObject();
            aleman.add("alemanX",px);
            aleman.add("alemanY",py);
            alemanes.add(aleman);
        }
        
        // AQui podiramos añadir mas info que le pasa el dron al interlocutor
        informacion.add("alemanes",alemanes);

                
        String content = informacion.toString();
        mandaMensaje(nombreInterlocutor, ACLMessage.INFORM , content);
            
        //vaciamos el vector de alemanes
        coordAleman.clear();
      
    }    
    
    
    
    /**
     * Realiza la función de refuel
     * Baja al suelo si fuese necesario, refuel y vuelve a subir
     * 
     * @param siguienteDireccion dirección a la que nos hemos movido
     * @param numero_pasos_bajar cantidad de movimientos que necesitamos para bajar al
     * suelo desde la posición inicial, si > 0, necesitamos bajar
     * 
     * @author David Infante Casas
     */
    public void refuel(String siguienteDireccion, int numero_pasos_bajar){
        JsonObject objeto = new JsonObject();
        String content = "";
        JsonObject respuesta;
        String resp = "";
        int i = numero_pasos_bajar;

        // Si estoy en el aire, bajo al suelo
        if (numero_pasos_bajar > 0) {
            // Hago un bucle con el número de pasos hasta llegar al suelo
            objeto.add("command", "moveDW");
            content = objeto.toString();

            // Bajo al suelo
            while (i > 0) {
                mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                recibeMensaje("Efectua movimiento mosca");
                this.replyWth = inbox.getReplyWith();
                if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
                     fuel = fuel - fuelrate;
                     //actualizamos la posicion localmente
                     actualizaPosicion(siguienteDireccion);
                }
                else{
                    respuesta = Json.parse(inbox.getContent()).asObject();            
                    resp = respuesta.get("result").asString();
                    System.out.println("Soy la mosca y no me he podido mover");
                    System.out.println(resp);
                    online = false;
                }

                --i;
            }

            // Si he tenido que bajar, borro el comando de bajar
            objeto.remove("command");
            cargarPercepciones();
        }

        // Reposto
        objeto.add("command","refuel");
        content = objeto.toString();
        mandaMensaje("Elnath", ACLMessage.REQUEST, content);

        recibeMensaje("Efectua refuel mosca");
        this.replyWth = inbox.getReplyWith();

        if(inbox.getPerformativeInt() == ACLMessage.INFORM){
             System.out.println("La mosca ha hecho refuel");
             cargarPercepciones();
        } else if (inbox.getPerformativeInt() == ACLMessage.FAILURE || inbox.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD){
            respuesta = Json.parse(inbox.getContent()).asObject();            
            resp = respuesta.get("result").asString();
            System.out.println(resp);
            online = false;
        }

        if (numero_pasos_bajar > 0) {
            // Borramos el comando refuel para volver a subir
            objeto.remove("command");
            objeto.add("command", "moveUP");
            content = objeto.toString();
            //Subo a la altura anterior
            i = numero_pasos_bajar;
            while (i > 0) {
                mandaMensaje("Elnath", ACLMessage.REQUEST, content);
                recibeMensaje("Efectua movimiento mosca");
                this.replyWth = inbox.getReplyWith();
                if (inbox.getPerformativeInt() == ACLMessage.INFORM) {
                     fuel = fuel - fuelrate;
                     //actualizamos la posicion localmente
                     actualizaPosicion(siguienteDireccion);
                }
                else{
                    respuesta = Json.parse(inbox.getContent()).asObject();            
                    resp = respuesta.get("result").asString();
                    System.out.println("Soy la mosca y no me he podido mover");
                    System.out.println(resp);
                    online = false;
                }

                --i;
            }
            cargarPercepciones();
        }
    }
    
    
}
