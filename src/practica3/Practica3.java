package practica3;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

/**
 * Clase main de la Práctica 2
 * 
 * @author Adrián Ruiz López
 * @author David Infante Casas
 * @author Mariana Orihuela Cazorla
 * @author Yang Chen
 */
public class Practica3 {

    /**
     * Método main del proyecto en el que
     * se conecta al agente con el servidor
     * 
     * @param args Argumentos de la línea de comandos
     * 
     * @author Adrián Ruiz López
     */
    public static void main(String[] args) {
        AgentsConnection.connect(
                "isg2.ugr.es",      // Servidor de la plataforma o localhost
                6000,               // Puerto de escucha
                "Practica3",        // VirtualHost
                "Eagle",            // Usuario
                "Hzrwtags",         // Password
                false               // Codificar las conexiones SSL
        );
        
        Interlocutor agente;
        String nombreInterlocutor = "Grupoe_";
        try {
            agente = new Interlocutor(new AgentID(nombreInterlocutor), "playground", true);

        } catch (Exception ex) {
            System.out.println("Error el agente ya existe en la plataforma");
            return;
        }
        
        
        agente.start();
        
    }//END MAIN   
    
}// END CLASS
