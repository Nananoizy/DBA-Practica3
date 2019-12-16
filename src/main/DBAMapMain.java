/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import DBAMap.DBAMap;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import java.io.File;
import java.util.Scanner;

/**
 *
 * @author lcv
 */
public class DBAMapMain {
    static String _filename="map500x500";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Reading json file "+"./json/"+_filename+".json");
        File file = new File("./json/"+_filename+".json");
        if (file != null)
            try {
                String str= new Scanner(file).useDelimiter("\\Z").next();
                /// START
                /// 1) A partir del JSONArray que me devuelve el INFORM de respuesta a SUBSCRIBE
                JsonArray img = Json.parse(str).asObject().get("map").asArray();
                DBAMap map = new DBAMap();
                /// 2) Construir una matriz bidimensional para el mapa
                map.fromJson(img);
                System.out.println("IMAGE DATA:");
                /// 3) Cuyas dimensiones se pueden consultar
                System.out.println(map.getWidth()+" pixs width & "+map.getHeight()+" pixs height");
                /// 4) Y cuyos valores se pueden consultar en getLevel(X,Y)
                System.out.print("First row starts with: ");
                for (int i=0; i<10; i++) 
                    System.out.print(map.getLevel(i, 0)+"-");
                System.out.print("\nLast row ends with: ");
                for (int i=0; i<10; i++) 
                    System.out.print(map.getLevel(map.getWidth()-1-i, map.getHeight()-1)+"-");
                System.out.println();
                /// END
                /// Se guarda una copia de la imagen en PNG, aunque esto no hace falta, es sólo a
                /// título informativo
                System.out.println("Saving file ./maps/"+_filename+".png");
                map.save("./maps/"+_filename+".png");
            }  catch (Exception ex) {
                System.err.println("***ERROR "+ex.toString());
            }
    }
    
}
