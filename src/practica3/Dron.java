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
    BufferedImage mapa;
    
    /**
     * Nombre del interlocutor.
     */
    String nombreInterlocutor = "Grupo_e";
    
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
    int posActualX,posActualY;
    
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
    int fuel;
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
        objetoJSON.add("dron", nombreDron);
        objetoJSON.add("posX", posActualX);
        objetoJSON.add("posY", posActualY);

        String content = objetoJSON.toString();
        
        System.out.println("Pidiendo siguiente posicion " + nombreDron);
            
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
     * Realiza las percepciones con el controlador
     * 
     * @author Adrian Ruiz Lopez
     */
    public void cargarPercepciones(){
        mandaMensaje("Elnath", ACLMessage.QUERY_REF ,"");
        recibeMensaje("mensaje de pedir Percepciones");
        
        if(inbox.getPerformativeInt() == ACLMessage.INFORM ){
            JsonObject objeto = Json.parse(inbox.getContent()).asObject();
            JsonObject result =  objeto.get("result").asObject();
            gps = result.get("gps").asObject();
            infrared = result.get("infrared").asArray();
            gonio = result.get("gonio").asObject();
            fuel = result.get("fuel").asInt();
            goal = result.get("goal").asBoolean();
            status = result.get("status").asString();
            awacs = result.get("awacs").asArray();
            torescue = result.get("torescue").asInt();
            energy = result.get("energy").asDouble();
            cancel =result.get("cancel").asBoolean();
            /*
            System.out.println("GPS -> "+gps);
            System.out.println("INFRAROJOS -> "+infrared);
            System.out.println("GONIO -> "+ gonio);
            System.out.println("FUEL -> "+ fuel);
            System.out.println("GOAL -> "+ goal);
            System.out.println("STATUS -> "+ status);
            System.out.println("AWACS -> "+ awacs);
            System.out.println("TORESCUE -> "+ torescue);
            System.out.println("ENERGY -> "+ energy);
            System.out.println("CANCEL -> "+ cancel);
            */
           // System.out.println("INFRAROJOS -> "+infrared);
        }
        
    }
    

    /*
    * Recorre el sensor de inflarojos y decuelve las posiciones de los alemanes
    */
    public void obtenerAlemanesInfrarojos(){
        List<JsonValue> lista = infrared.values();       
        List<Integer> posi = new ArrayList<Integer>();
        System.out.println("N=" + range);
        int anchura = range-1;
        int radio = anchura/2;
        
        for( int y=0;y<range;y++){
            for(int x=0;x<range;x++){
                
                if( lista.get((y*anchura)+x).asInt() == 1 ){
                       posi.add(x);
                       posi.add(y);
                }
                if ( y==radio && x== radio ) System.out.print("D ");
                else System.out.print(lista.get((y*anchura)+x) + " ");
            }
            System.out.println("");
        }
        
        System.out.println(gps);
        System.out.println(posi);
        
        
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
            
            System.out.println("Aleman -> (" + x + "," + y + ")" );
            
            Pair<Integer,Integer> aleman = new Pair(x,y);
            coordAleman.add(aleman);
            
        }
       /*
        for(int i=0; i<coordAleman.size();i++){
           int px=coordAleman.get(i).getKey();
           int py=coordAleman.get(i).getValue();
           System.out.println("Aleman " + i + " en la coordenada x= " + px + " , y = " + py);
        }
        */
            

    }// fin obetenerAlemanesInfrarojos
    
    
    public void mandarCoordenadas(){
        while(coordAleman.size()>0){
            for(int i=0;i<coordAleman.size();i++){
                int px=coordAleman.get(i).getKey();
                int py=coordAleman.get(i).getValue();
                
                JsonObject objetoJSON = new JsonObject();
                objetoJSON.add("comando", "aniadir");
                objetoJSON.add("posicionx",px);
                objetoJSON.add("posiciony",py);
                
                String content = objetoJSON.toString();
                
                mandaMensaje(nombreInterlocutor, ACLMessage.REQUEST , content);
                 
                System.out.println("voy a mandar las coordenadas al interlocutor");
           
                recibeMensaje("mensaje de mandar posiciones de almanes");
            }
            
              //vaciamos el vector de alemanes
              coordAleman.clear();
        }
        
<<<<<<< Updated upstream
=======
        // AQui podiramos añadir mas info que le pasa el dron al interlocutor
        informacion.add("alemanes",alemanes);

                
        String content = informacion.toString();
        mandaMensaje(nombreInterlocutor, ACLMessage.INFORM , content);
            

>>>>>>> Stashed changes
      
    }    
    
    
    
    /**
    * Método DE EJEMPLO QUE NOS SERVIRÁ DESPUES (SI NO BORRAR)
    * 
    * 
    * @author Adrián Ruiz Lopez
    */
    public void metodo( String dir ){
        
        switch(dir){
            case "N":
                
                break;
            case "NW":
                
                break;
            case "W":
                
                break;
            case "S":
                
                break;
            case "SE":
                
                break;
            case "E":
                
                break;
            case "NE":
                
                break;
            case "SW":
                
                break;
        }
        
        
    }
    
    
    
}
