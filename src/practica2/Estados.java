package practica2;

/**
 * Estados en los que se puede encontrar el agente
 * 
 * @author David Infante Casas
 */
public enum Estados {
    
    /**
     * Ejecutando el comportamiento del agente
     */
    CREANDO,
    
    /**
     * Inicialización del agente
     */
    INICIALIZANDO,
    
    /**
     * Haciendo login
     */
    LOGEANDO,
    
    /**
     * Finalizando el comportamiento del agente
     */
    FINALIZANDO,
    
    /**
     * Uso de los sensores para percibir
     */
    PERCIBIENDO,
    
    /**
     * Repostando
     */
    REFUEL,
    
    /**
     * Aterrizando para repostar
     */
    ATERRIZANDO,
    
    /**
     * Abortando la misión
     */
    ABORTANDO
}
