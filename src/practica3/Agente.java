package practica3;

import DBA.SuperAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase que define al agente, su comportamiento, sensores y comunicaciones
 * 
 * @author Adrián Ruiz López
 * @author David Infante Casas
 * @author Mariana Orihuela Cazorla
 * @author Yang Chen
 */
public class Agente extends SuperAgent {
    
    /**
     * True si estamos logueados, false en otro caso.
     */
    boolean logged;
    /**
     * Iteraciones máximas para evitar un bucle infinito.
     */
    private final int MAX_STEPS = 2500;
    /**
     * Estado actual del agente.
     */
    Estados estado;
    /**
     * Mapa que recorre el agente.
     */
    String mapaActual;
    /**
     * Clave de sesión para hacer login y logout.
     */
    String key;
    /**
     * Dimensiones y alturas del mapa.
     */
    int dimX, dimY, alturaMin, alturaMax;
    
    /* Sensores */
    
    /**
     * Sensor GOAL,
     * goal True si se ha llegado al objetivo, false en otro caso.
     */
    boolean goal;
    /**
     * Sensor GONIO,
     * distanciaRestante Distancia hasta el objetivo
     * angulo Ángulo en el que se encuentra el objetivo.
     */
    float distanciaRestante; float angulo;
    /**
     * Sensor RADAR,
     * miRadar Array de valores de una matrix 11x11 en la que se muestran
     * los valores globales de la altura.
     */
    ArrayList<Integer> miRadar;
    /**
     * Sensor ELEVATION,
     * miElevation Array de valores de una matrix 11x11 en la que se muestran
     * los valores de la altura según la altura relativa al agente.
     */
    ArrayList<Integer> miElevation;
    /**
     * Sensor MAGNETIC,
     * miMagnetic Array de valores de una matrix 11x11 en la que se muestran
     * como 1's las posiciones objetivo.
     */
    ArrayList<Integer> miMagnetic;
    /**
     * Cantidad de Combustible (FUEL),
     * fuel Indica la cantidad de fuel de la que dispone el agente.
     */
    float fuel;
    /**
     * Sensor Status
     * status Indica el estado del agente, operativo o crashed.
     */
    String status;
    
    /* Memoria */
    
    /**
     * Almacenamiento de las acciones realizadas anteriormente.
     */
    ArrayList<String> ultimaAccion;
    
    /**
     * Variable que se activa cuando el dron aborta la mision actual.
     */
    boolean abortar;
   
    /**
     * Memoria que almacena las diferentes direcciones a las que el agente ha obtenido del GONIO.
     */
   ArrayList<String> ultimasDirecciones;

   
    
    /**
     * Crea un nuevo Agente
     * 
     * @param aid ID del agente
     * @param mapa Mapa que va a recorrer el agente
     * @throws Exception
     * 
     * @author Adrián Ruiz Lopez
     */
    public Agente(AgentID aid, String mapa) throws Exception {
        super(aid);
        estado = Estados.CREANDO;
        mapaActual = mapa;
        miRadar = new ArrayList<>();
        miElevation= new ArrayList<>();
        miMagnetic = new ArrayList<>();
        ultimaAccion = new ArrayList<>();
        ultimaAccion.add("inicio");
        ultimasDirecciones = new ArrayList<>();
        ultimasDirecciones.add("inicio");
        abortar = false;
    }
    
    
    
    /**
     * Inicialización del agente
     * 
     * @author Adrian Ruiz Lopez
     * @author David Infante Casas
     */
    @Override
    public void init() {
        logged = false;
        estado = Estados.INICIALIZANDO;
    }
    
    
    
    /**
     * Comportamiento del agente
     * 
     * @author Adrian Ruiz Lopez
     * @author David Infante Casas
     */
    @Override
    public void execute() {        
        // Hacemos login
        estado = Estados.LOGEANDO;
        login(mapaActual);
        
        logged = recibirLogin();
        
        if (logged) {            
            estado = Estados.PERCIBIENDO;
            percibir();
           
            // Bucle de acción del agente
            for (int i = 0; i < MAX_STEPS && !goal; ++i) {
                
                if( abortar && miElevation.get(60) == 0 ) break;
                
                System.out.println("\nIteración " + i);
                nextAction();
            }
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
        if( abortar ) estado = Estados.ABORTANDO;
        else estado = Estados.FINALIZANDO;
        

        if (logged) logout(this.key);
        if (goal) System.out.println("FIN : He llegado a mi destino!!");
        else if (abortar) System.out.println("FIN : Mision Abortada!!");
        
        ACLMessage inbox;
        
        try {
            inbox = this.receiveACLMessage();
            String recibirOk = inbox.getContent();
            
            JsonObject objeto = Json.parse(recibirOk).asObject();
           
            //si hemos finalizado con exito, saca la traza de ejecucion
            if (objeto.get("result").asString().equals("ok")){
                extraerTraza();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(Agente.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
        
    
    /**
     * Recepción del mensaje login con los parámetros
     * 
     * @return true En el caso de que se hayan
     * recibido los parámetros, false en otro caso
     * 
     * @author Adrian Ruiz Lopez
     */
    public boolean recibirLogin() {
        try {
            ACLMessage inbox = this.receiveACLMessage();
            String loginRecibido = inbox.getContent();
            
            JsonObject objeto = Json.parse(loginRecibido).asObject();
           
            if (objeto.get("result").asString().equals("ok") && objeto.get("in-reply-to").asString().equals("login")) {
                key = objeto.get("key").asString();
                dimX = objeto.get("dimx").asInt();
                dimY = objeto.get("dimy").asInt();
                alturaMin = objeto.get("min").asInt();
                alturaMax = objeto.get("max").asInt();
                System.out.println("ESTOY LOGUEADO");
                return true;
            } else {
                System.out.println("LOGIN ERROR!");
                return false;
            }
        } catch (InterruptedException ex) {
            System.out.println("Error al recibir el mensaje");
            return false;
        }
    }
    
    
    
    /**
     * Envío del mensaje para hacer login
     * 
     * @param mapa Mapa al que se quiere acceder
     * 
     * @author Adrian Ruiz Lopez
     */
    public void login(String mapa) {
        /* Preparación del Mensaje */
        JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("command","login");
        objetoJSON.add("map",mapa);
        objetoJSON.add("radar", true);
        objetoJSON.add("elevation", true);
        objetoJSON.add("magnetic", true);
        objetoJSON.add("gps", true);
        objetoJSON.add("fuel", true);
        objetoJSON.add("gonio", true);
        objetoJSON.add("user", "Eagle");
        objetoJSON.add("password", "Hzrwtags");
        
        String mensaje = objetoJSON.toString();
        
        /* envío */
        ACLMessage outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Elnath"));
        outbox.setContent(mensaje);
        this.send(outbox);
    }
    
    
    
    /**
     * Envío del mensaje para hacer logout
     * 
     * @param key Clave de la sesión
     * 
     * @author Adrian Ruiz Lopez
     */
    public void logout(String key) {
        /* Preparación del Mensaje */
        JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("command","logout");
        objetoJSON.add("key",key);
        
        String mensaje = objetoJSON.toString();
        
        /* envío */
        ACLMessage outbox = new ACLMessage(); 
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Elnath"));
        outbox.setContent(mensaje);
        this.send(outbox);
    }
    
    
    
    /**
     * Se guarda y se muestra la información recibida de los sensores
     * 
     * @return true si la ejecución ha ido mal o false en caso contrario
     * 
     * @author Adrián Ruiz López
     * @author Mariana Orihuela Cazorla
     * @author David Infante Casas
     */
    public boolean percibir() {
        try {
            ACLMessage inbox = this.receiveACLMessage();
            String mensaje = inbox.getContent();
            
            JsonObject objeto = Json.parse(mensaje).asObject();
            //System.out.println( "\n" + objeto.toString());
            
            JsonObject percepciones = objeto.get("perceptions").asObject();
            //System.out.println( "\n" + percepciones.toString());
            //System.out.println();            
            //introducimos los datos del sensor GOAL
            this.goal = percepciones.get("goal").asBoolean();
            System.out.println( "Goal:" + goal);
            
            //introducimos los datos del sensor GONIO
            this.distanciaRestante = percepciones.get("gonio").asObject().get("distance").asFloat();
            System.out.println( "Distancia restante:" + distanciaRestante);
            this.angulo = percepciones.get("gonio").asObject().get("angle").asFloat();
            System.out.println( "Angulo:" + angulo);

            // introducimos los datos del sensor de batería
            this.fuel = percepciones.get("fuel").asFloat();
            System.out.println( "Fuel:" + fuel);
            
            //introducimos los datos del sensor de status
            this.status = percepciones.get("status").asString();
            //System.out.println( "Status:" + status);
            
            int i = 0;
            // Introducimos los datos del radar en nuestro radar.
            this.miRadar.clear();
            //System.out.println( "Radar: [");
            for( JsonValue j : percepciones.get("radar").asArray() ) {
                this.miRadar.add(j.asInt());
                //System.out.print(j.asInt() + ", ");
                i++;
                if (i == 11){
                    //System.out.println();
                    i = 0;
                }
            }
            //System.out.println("]");
            
            i = 0;
            // Introducimos los datos de la elevacion.
            this.miElevation.clear();
            //System.out.println( "Elevacion: [");
            for( JsonValue j : percepciones.get("elevation").asArray() ) {
                this.miElevation.add(j.asInt());
                //System.out.print(j.asInt() + ", ");
                i++;
                if (i == 11){
                    //System.out.println();
                    i = 0;
                }
            }
            //System.out.println("]");
            
            i = 0;
            // Introducimos los datos del magnetic en miMagnetic.
            this.miMagnetic.clear();
            //System.out.println( "Magnetic: [");
            for( JsonValue j : percepciones.get("magnetic").asArray() ) {
                this.miMagnetic.add(j.asInt());
                //System.out.print(j.asInt() + ", ");
                i++;
                if (i == 11){
                    //System.out.println();
                    i = 0; 
                } 
            }
           // System.out.println("]");
           
           
           System.out.println("\nMi estado actual es :" + estado);
           
           return true;
                     
        } catch (InterruptedException ex) {
            System.out.println("Error al recibir el mensaje");
            return false;
        }
    }
    
    
    
    /**
     * Manda al servidor la siguiente acción a realizar y
     * recibe el ok o un mensaje de error
     * 
     * @return true si hemos recibido un ok de la acción
     * false si hemos enviado una acción erronea o ha habido un error
     * 
     * @author David Infante Casas
     */
    private boolean nextAction() {
        String comando = nextCommand();
        System.out.println("\n\n" + comando);
        // Mandamos el mensaje con la acción a realizar
        JsonObject objetoJSON = new JsonObject();
        objetoJSON.add("command", comando);
        objetoJSON.add("key", this.key);
        
        String mensaje = objetoJSON.toString();

        ACLMessage outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Elnath"));
        outbox.setContent(mensaje);
        this.send(outbox);
        
        // Recibimos el mensaje de ok
        try {
            ACLMessage inbox = this.receiveACLMessage();
            String recibirOk = inbox.getContent();
            
            JsonObject objeto = Json.parse(recibirOk).asObject();
           
            if (objeto.get("result").asString().equals("ok") && objeto.get("in-reply-to").asString().equals(comando)) {
                System.out.println("Percibiendo la nueva posición");
                percibir();
                return true;
            }else  if (objeto.get("result").asString().equals("CRASHED") && objeto.get("in-reply-to").asString().equals(comando)) {
                System.out.println("He chocado con un obstaculo o no tengo mas bateria");
                return false;
             }else  if (objeto.get("result").asString().equals("BAD COMMAND") && objeto.get("in-reply-to").asString().equals(comando)) {
                System.out.println("comando invalido");
                System.out.println("El comando enviado es " + comando);
                return false;
             }else  if (objeto.get("result").asString().equals("BAD MESSAGE OR BAD JSON") && objeto.get("in-reply-to").asString().equals("N/A")) {
                System.out.println("El mensaje o objeto Json invalido");
                return false;
             }else  if (objeto.get("result").asString().equals("BAD KEY") && objeto.get("in-reply-to").asString().equals(comando)) {
                System.out.println("key incorrecto");
                return false;
             }else{
                System.out.println("Error al percibir");
                return false;
            }
        } catch (InterruptedException ex) {
            System.out.println("Error al recibir el mensaje");
            return false;
        }
    }
    
    
    
    /**
     * Algoritmo que busca mediante la información obtenida de
     * los sensores, el movimiento óptimo a realizar en la
     * siguiente acción
     * 
     * @return command Comando a realizar en la siguiente acción
     * 
     * @author David Infante Casas
     * @author Mariana Orihuela Cazorla
     * @author Adrian Ruiz Lopez
     * @author Yang Chen
     */
    private String nextCommand() {
        System.out.println("****************************" + "\n" + "Siguiente comando:");
        String command = "error";
        String direccion = "null";
        
            
        // Si estamos en el estado repostar, bajamos y repostamos
        if (estado == Estados.REFUEL) {
            if (miElevation.get(60) > 0) {
                estado = Estados.REFUEL;
                command = "moveDW";
            }
            else {
                estado = Estados.PERCIBIENDO;
                command = "refuel";
            }
        }else{
            
            // Si estamos sobre el objetivo con fuel suficiente, bajamos
            if (miMagnetic.get(60) == 1 && (fuel/0.5) > (miElevation.get(60)/5)  && miElevation.get(60) > 0) {
                estado = Estados.ATERRIZANDO;
                command = "moveDW";
            
            // Si ABORT está a true
            } else if (abortar) {
                // si estamos elevados, bajamos
                if ( miElevation.get(60) > 0 ) {
                    estado = Estados.ABORTANDO;
                    command = "moveDW";
                }
                
            // Si no hemos llegado al objetivo ni abortado la misión
            } else {
               //  Comprobamos si tenemos fuel
               double fuelNecesario = ((miElevation.get(60)/5)*0.5); // número de pasos * 0,5 de fuel en cada paso
               if (fuel-fuelNecesario < 5.0) { // que por lo menos tenga un margen de 5% de bateria, es decir 10 pasos
                   command = accionInversa(ultimaAccion.get(ultimaAccion.size()-1));
                   estado = Estados.REFUEL;

               // Si tenemos fuel, ejecutamos el algoritmo de movimiento estandar
               } else {  
                    // Voy a ir a cada dirección si es la mejor y si no he ido antes a ella
                    if ((angulo >= 337.5 ) || (angulo < 22.5)) {
                        if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveS")) {
                            direccion = manoDerecha("NW");
                            command =  "move"+direccion;

                        } else {
                            direccion = "N";
                        }
                    }
                    else if (angulo >= 22.5 && angulo < 67.5) {
                        if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveSW")) {
                            direccion= manoDerecha("N");
                            command = "move"+direccion;
                        } else {
                            direccion = "NE";
                        }
                    }
                    else if (angulo >= 67.5 && angulo < 112.5) {
                        if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveW")) {
                            direccion = manoDerecha("NE");
                            command = "move"+direccion;
                        } else {
                            direccion = "E";
                        }
                    }
                    else if (angulo >= 112.5 && angulo < 157.5) {
                        if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveNW")) {
                            direccion = manoDerecha("E");
                            command = "move"+direccion;
                        } else {
                            direccion = "SE";
                        }
                    }
                    else if (angulo >= 157.5 && angulo < 202.5) {
                        if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveN")) {
                            direccion = manoDerecha("SE");
                            command = "move"+direccion;
                        } else {
                            direccion = "S";
                        }
                    }
                    else if (angulo >= 202.5 && angulo < 247.5) {
                        if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveNE")) {
                            direccion = manoDerecha("S");
                            command = "move"+direccion;
                        } else {
                            direccion = "SW";
                        }
                    }
                    else if (angulo >= 247.5 && angulo < 292.5) {
                        if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveE")) {
                            direccion = manoDerecha("SW");
                            command = "move"+direccion;

                        } else {
                            direccion = "W";
                        }
                    }
                    else if (angulo >= 292.5 && angulo < 337.5) {
                         if (ultimaAccion.get(ultimaAccion.size()-1).equals("moveSE")) {
                            direccion = manoDerecha("W");
                            command = "move" + direccion;
                         } else {
                            direccion = "NW";
                         }
                    }
                   
                    /********************************************************************/
                    if (!ultimasDirecciones.get(ultimasDirecciones.size()-1).equals(direccion)) {
                        ultimasDirecciones.add(direccion);
                    }

                    int vueltas = 2*8;
                    if (ultimasDirecciones.size() >= vueltas) {
                        System.out.print("ULTIMAS DIRECCIONES: ");
                        int n = ultimasDirecciones.size()-1;
                        for (int i = 0; i < vueltas; ++i) {
                            System.out.print(n-i + ": " + ultimasDirecciones.get( n-i ) + " ");
                        }
                        System.out.println("");

                        boolean ciclo = true;
                        n = ultimasDirecciones.size()-1;
                        String vecino1, vecino2;
                        for (int i = 0; i < vueltas && ciclo; ++i) {
                            vecino1 = ultimasDirecciones.get(n-i);
                            vecino2 = ultimasDirecciones.get(n-((i+1)%vueltas));
                            //System.out.print( n-i + "->" + ( n-( (i+1)%vueltas) ) + "\t" + "v1: " + vecino1 + " con  v2: " + vecino2 + "\n" );
                            if (!vecino2.equals(vecino(vecino1))) {
                             ciclo = false;
                            }
                        }

                        if (ciclo) {
                            abortar = true;
                        }
                   }// end if de minimo 8 direcciones
                                      
                    /********************************************************************/                 

                    // si el comando es "error" significa que no hemos eleguido un comando aun
                    // si no entramos en este if, significa que hemos decidido ya 1 comando.
                    if (command.equals("error")) {
                        System.out.println("SE HA REALIZADO UN PASO NORMAL");
                        command = selectComandForDirection( direccion);
                    } else {
                        System.out.println("SE HA REALIZADO CON MANO DERECHA");
                    }
                   
               }//end else ( MOVIMIENTO ESTARDAR)
                
            }// ELSE SI NO HA LLEGADO AL OBJETIVO     
            
        }// end else NO REFUEL
        
        ultimaAccion.add(command);
        
        System.out.println("\nEl siguiente comando es el :" + command);
        
        return command;
    }
    
    
    
    /**
     * Selecciona el comando que debemos hacer a continuación
     * 
     * @param direc Dirección óptima a la que deberíamos movernos
     * @return command Comando con la acción que debemos realizar
     * 
     * @author David Infante Casas
     * @author Mariana Orihuela Cazorla
     */
    String selectComandForDirection(String direc) {
        String command = "error";
        switch (direc) {
            case "N":
                // Subir si el paso siguiente está más alto AND es menor que la altura máxima
                if (miElevation.get(49) < 0 && miRadar.get(49) <= alturaMax) command = "moveUP";
                // Avanzar si el siguiente paso está más bajo o a la misma altura AND la posición siguiente es mayor que 0
                else if ((miElevation.get(49) >= 0) && (miRadar.get(49) > 0)) command = "moveN";
                //si el siguiente está más alto que lo permitido, usamos regla de mano derecha
                //tenemos que cambiar de direccion tantas veces como obstaculos haya
                else if ((miElevation.get(49) < 0 && miRadar.get(49) > alturaMax)){
                    command = "move"+manoDerecha("N");
                } 
                break;
            case "NE":
                if (miElevation.get(50) < 0 && miRadar.get(50) <= alturaMax) command = "moveUP";
                else if ((miElevation.get(50) >= 0) && (miRadar.get(50) > 0)) command = "moveNE";
                else if ((miElevation.get(50) < 0 && miRadar.get(50) > alturaMax)){
                    command = "move"+manoDerecha("NE");
                }
                break;
            case "E":
                if (miElevation.get(61) < 0 && miRadar.get(61) <= alturaMax) command = "moveUP";
                else if ((miElevation.get(61) >= 0) && (miRadar.get(61) > 0)) command = "moveE";
                else if ((miElevation.get(61) < 0 && miRadar.get(61) > alturaMax)){
                    command = "move"+manoDerecha("E");
                }
                break;
            case "SE":
                if (miElevation.get(72) < 0 && miRadar.get(72) <= alturaMax) command = "moveUP";
                else if ((miElevation.get(72) >= 0) && (miRadar.get(72) > 0)) command = "moveSE";
                else if ((miElevation.get(72) < 0 && miRadar.get(72) > alturaMax)){
                    command = "move"+manoDerecha("SE");
                }
                break;
            case "S":
                if (miElevation.get(71) < 0 && miRadar.get(71) <= alturaMax) command = "moveUP";
                else if ((miElevation.get(71) >= 0) && (miRadar.get(71) > 0)) command = "moveS";
                else if ((miElevation.get(71) < 0 && miRadar.get(71) > alturaMax)){
                    command = "move"+manoDerecha("S");
                }
                break;
            case "SW":
                if (miElevation.get(70) < 0 && miRadar.get(70) <= alturaMax) command = "moveUP";
                else if ((miElevation.get(70) >= 0) && (miRadar.get(70) > 0)) command = "moveSW";
                else if ((miElevation.get(70) < 0 && miRadar.get(70) > alturaMax)){
                    command = "move"+manoDerecha("SW");
                }
                break;
            case "W":
                if (miElevation.get(59) < 0 && miRadar.get(59) <= alturaMax) command = "moveUP";
                else if ((miElevation.get(59) >= 0) && (miRadar.get(59) > 0)) command = "moveW";
                else if ((miElevation.get(59) < 0 && miRadar.get(59) > alturaMax)){
                    command = "move"+manoDerecha("W");
                }
                break;
            case "NW":
                if (miElevation.get(48) < 0 && miRadar.get(48) <= alturaMax) command = "moveUP";
                else if ((miElevation.get(48) >= 0) && (miRadar.get(48) > 0)) command = "moveNW";
                else if ((miElevation.get(48) < 0 && miRadar.get(48) > alturaMax)){
                    command = "move"+manoDerecha("NW");
                }
                break;
        }//end swich
        return command;
    }
    
    
    
    /**
     * Selecciona el vecino por la izquierda de la dirección pasada
     * 
     * @param actual Dirección de la que queremos obtener el vecino
     * @return vecino Vecino de la dirección pasada
     * 
     * @author Adrián Ruiz López
     */
    private String vecino(String actual) {
        String vecino = "";
        switch(actual){
            case "N":
                vecino = "NW";
                break;
            case "NW":
                vecino = "W";
                break;
            case "W":
                vecino = "SW";
                break;
            case "S":
                vecino = "SE";
                break;
            case "SE":
                vecino = "E";
                break;
            case "E":
                vecino = "NE";
                break;
            case "NE":
                vecino = "N";
                break;
            case "SW":
                vecino = "S";
                break;
        }
        return vecino;
    }

    
    
    /**
     * Devuelve el vecino en sentido antihorario de la dirección introducida hasta que no haya muros
     * 
     * @param inicial Dirección de la que queremos obtener el vecino
     * @return vecino Vecino de la dirección actual en sentido antihorario
     * 
     * @author Mariana Orihuela Cazorla
     */
    private String manoDerecha(String inicial) {
        
        boolean puedoAvanzar = false;
        
        System.out.println("Algoritmo de mano derecha");
                
        // Al principio la primera direccion que se intente sera a la que ibamos a avanzar
        String actual = inicial;
        // Contador se encarga de controlar cuando de una vuelta completa
        Integer contador = 0;
        
        // Mientras que no encuentre una salida o no haya dado ya una vuelta, ciclo
        while ((!puedoAvanzar) && (contador < 8)) {
            System.out.println("Comando actual: " + actual);
            switch (actual) {
                case "N":
                    // Si la altura no es max y no es el limite del mapa, se puede avanzar; si lo es, pasamos al siguiente
                    if ((miRadar.get(49) > alturaMax) || (miRadar.get(49) == 0)) {
                        actual = "NW";
                    }
                    // Si la casilla a la que quiero avanzar está más alta, tengo que ascender
                    else if (miElevation.get(49) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
                case "NW":
                    if ((miRadar.get(48) > alturaMax) || (miRadar.get(48) == 0)) {
                        actual = "W";
                    }
                    else if (miElevation.get(48) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
                case "W":
                    if ((miRadar.get(59) > alturaMax) || (miRadar.get(59) == 0)) {
                        actual = "SW";
                    }
                    else if (miElevation.get(59) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
                case "SW":
                    if ((miRadar.get(70) > alturaMax) || (miRadar.get(70) == 0)) {
                        actual = "S";
                    }
                    else if (miElevation.get(70) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
                case "S":
                    if ((miRadar.get(71) > alturaMax) || (miRadar.get(71) == 0)) {
                        actual = "SE";
                    }
                    else if (miElevation.get(71) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
                case "SE":
                    if ((miRadar.get(72) > alturaMax) || (miRadar.get(72) == 0)) {
                        actual = "E";
                    }
                    else if (miElevation.get(72) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
                case "E":
                    if ((miRadar.get(61) > alturaMax) || (miRadar.get(61) == 0)) {
                        actual = "NE";
                    }
                    else if (miElevation.get(61) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
                case "NE":
                    if ((miRadar.get(50) > alturaMax) || (miRadar.get(50) == 0)) {
                        actual = "N";
                    }
                    else if (miElevation.get(50) < 0) {
                        actual = "UP";
                        puedoAvanzar = true;
                    }
                    else{
                        puedoAvanzar = true;
                    }
                    contador++;
                    break;
            }
        }
        
        return actual;
    }
    
    
    
    /**
     * Devuelve la acción inversa de la pasada en accion
     * 
     * @param accion Accion de la que queremos obtener su inversa
     * @return inversa Dirección inversa de actual
     * 
     * @author Adrián Ruiz López
     */
    public String accionInversa(String accion) {
        String inversa = "";
        switch (accion) {
            case "moveN":
                inversa = "S";
                break;
            case "moveNW":
                inversa = "SE";
                break;
            case "moveW":
                inversa = "E";
                break;
            case "moveS":
                inversa = "N";
                break;
            case "moveSE":
                inversa = "NW";
                break;
            case "moveE":
                inversa = "W";
                break;
            case "moveNE":
                inversa = "SW";
                break;
        }
        return "move" + inversa; 
    }
    
    
    
    /**
     * Método toString de la clase
     * 
     * @return Devuelve el string
     * 
     * @author Adrián Ruiz López
     * @author Yang Chen
     */    
    @Override
    public String toString() {
        return "\nkey: " + key + "\ndimX: " + dimX + "\ndimY: " + dimY + "\nAlturaMIN: " + alturaMin + "\nAlturaMAX: " + alturaMax ;
    }

    
    
     /**
     * Método que extrae la traza de ejecucion a una imagen PNG que se guarda en el disco
     * 
     * @author Mariana Orihuela Cazorla
     */    
    public void extraerTraza() {
        try {
            ACLMessage inbox = this.receiveACLMessage();
            JsonObject injson = Json.parse(inbox.getContent()).asObject();
            JsonArray ja = injson.get("trace").asArray();
            byte data[] = new byte [ja.size()];
            for (int i = 0; i < data.length; i++){
                data[i] = (byte) ja.get(i).asInt();
            }
            FileOutputStream fos = new FileOutputStream("mitraza.png");
            fos.write(data);
            fos.close();
            System.out.println("Traza guardada");
            System.out.println("las direcciones tiene " + ultimasDirecciones.size());
        } catch(InterruptedException | IOException ex) {
            System.err.println ("Error procesando traza");
        }   
    }
  
}
