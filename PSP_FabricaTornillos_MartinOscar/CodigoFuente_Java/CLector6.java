import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Oscar Martin Hernandez
 * 22/10/2026
 * Modulo: PSP
 * Clase CLector6
 * Esta clase representa un lector de pedidos de la fabrica
 */

/*
SOLICITUD_PRODUCCION_SCADA_V2
EMISOR_CIF: B84551209
EMISOR_EMPRESA: REA_ROBOTICA
ORDEN_IDENTIFICADOR: REA-2026-0701
TIMESTAMP: 2026-10-18T10:40:22
COMANDO_EJECUCION: FABRICAR
PARAMETRO_TIPO_TORNILLO: T3
PARAMETRO_CANTIDAD_TORNILLOS: 200
PARAMETRO_ENVASE_FINAL: C
MODO_FABRICACION: CONTINUO
FIN_SOLICITUD
 */

public class CLector6 {

    // Metodo principal que recibe la ruta del archivo y procesa su contenido
    public static void main(String[] args) {

        // Compruebo que se haya indicado exactamente un argumento: la ruta del archivo
        if(args.length != 1){
            System.err.println("Error: Debes indicar la ruta del archivo");
            System.exit(1); // Termina el proceso Java indicando que ha habido un error
        }

        //Flujo de lectura 
        try(BufferedReader oLector = new BufferedReader(new FileReader(args[0]))){
            
            String sLinea;
            String sIdPedido= ""; // Guarda el identificador del pedido
            String sTipoTornillo="";   // Guarda el tipo de tornillo que se ha pedido
            String sCantidad= "";   // Guarda la cantidad
            String sFormatoEmpaque=""; // Guarda el formato de empaquetado que se ha pedido

            while((sLinea=oLector.readLine()) != null){
                
                //Busco los datos del pedido   
                if(sLinea.startsWith("ORDEN_IDENTIFICADOR")){
                    // La primera parte contiene el nombre del campo y la segunda su valor
                    String[] aDatos = sLinea.split(":",2);

                    // Guardo el identificador
                    sIdPedido=aDatos[1].trim();
                }

                if(sLinea.startsWith("PARAMETRO_TIPO_TORNILLO")){
                    String[] aDatos = sLinea.split(":",2);

                    // Guardo el tipo de tornillo
                    sTipoTornillo=aDatos[1].trim();
                }

                if(sLinea.startsWith("PARAMETRO_CANTIDAD_TORNILLOS")){
                    String[] aDatos = sLinea.split(":",2);

                    // Guardo la cantidad
                    sCantidad=aDatos[1].trim();
                }
                
                if(sLinea.startsWith("PARAMETRO_ENVASE_FINAL")){
                    String[] aDatos = sLinea.split(":",2);

                    // Guardo el formato de empaquetado
                    sFormatoEmpaque=aDatos[1].trim();
                }           
            }

            //Compruebo que se hayan encontrado todos los datos
            if(sIdPedido.isEmpty() || sTipoTornillo.isEmpty() || sCantidad.isEmpty() || sFormatoEmpaque.isEmpty()){
                System.err.println("Error: Faltan datos del pedido");
                System.exit(1);
            }

            int iCantidad = Integer.parseInt(sCantidad);

            //Compruebo que los datos del pedido sean correctos
            if(!sIdPedido.isEmpty() && iCantidad >0 && (sTipoTornillo.equals("T1") || sTipoTornillo.equals("T2") || sTipoTornillo.equals("T3")) && (sFormatoEmpaque.equals("D") || sFormatoEmpaque.equals("P") || sFormatoEmpaque.equals("C"))){
                System.out.println(sIdPedido + ";" + sTipoTornillo + ";" + iCantidad + ";" +sFormatoEmpaque);
            }else{
                System.err.println("Error: formato de lienas incorrecto: " + sLinea);
                System.exit(1);
            }                           

        } catch (IOException e) { // Excepcion por si ocurre algun problema al abrir/leer el archivo
            System.err.println("Error al leer el archivo");
            System.exit(1);
        }

    }
}