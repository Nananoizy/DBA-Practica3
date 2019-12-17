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
    String nombreInterlocutor = "Grupoe_prueba  ";
    
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
        System.out.println("El reply mandado es " + replyWth);
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
        
        System.out.println("El reply recibido es " + inbox.getReplyWith().toString());
        System.out.println("mi reply actual es " + replyWth);
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
        int x_actual = gps.get("x").asInt();
        int y_actual = gps.get("y").asInt();
              
        // Según el ángulo calculamos la posición
       if (angulo >= 0 && angulo <= 90) { // N-E
            x_aleman = (int) (x_actual + Math.cos(angulo) * distancia);
            y_aleman = (int) (y_actual - Math.sin(angulo) * distancia);
        } else if (angulo > 90 && angulo <= 180) { // E-S
            x_aleman = (int) (x_actual + Math.cos(angulo) * distancia);
            y_aleman = (int) (y_actual + Math.sin(angulo) * distancia);
        } else if (angulo > 180 && angulo <= 270) { // S-W
            x_aleman = (int) (x_actual - Math.cos(angulo) * distancia);
            y_aleman = (int) (y_actual + Math.sin(angulo) * distancia);
        } else if (angulo > 270 && angulo <= 360) { // W-N
            x_aleman = (int) (x_actual - Math.cos(angulo) * distancia);
            y_aleman = (int) (y_actual - Math.sin(angulo) * distancia);
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
    
    
    

    
    
    
}
