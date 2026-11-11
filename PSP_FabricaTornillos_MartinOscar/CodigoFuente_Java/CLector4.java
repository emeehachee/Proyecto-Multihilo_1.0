import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Oscar Martin Hernandez
 * 21/10/2026
 * Modulo: PSP
 * Clase CLector4
 * Esta clase representa un lector de pedidos de la fabrica
 */

/*
EMP|A39871120|Suministros Navales del Norte S.A.|SANTANDER-PUERTO|ES
ORD|SNN-2026-1120|20261018|103000|NORMAL
LIN|01|T1|300|C|TORNILLO_ACERO_INOX_MARINO
TRA|CONTROL_CHECKSUM=VALIDO|TOTAL_ITEMS=1
 */

public class CLector4 {
    
    // Metodo principal que recibe la ruta del archivo y procesa su contenido
    public static void main(String[] args) {

        // Compruebo que se haya indicado exactamente un argumento: la ruta del archivo
        if(args.length != 1){
            System.err.println("Error: Debes indicar la ruta del archivo");
            System.exit(1); // Termina el proceso Java indicando que ha habido un error
        }

        try(BufferedReader oLector = new BufferedReader(new FileReader(args[0]))){
            
            String sLinea;
            String sIdPedido= "";  // Guardo el identificador del pedido encontrado en el registro de ORD
            boolean bPedidoEncontrado = false; // Indica si se ha encontrado el registro del pedido
            boolean bLineaEncontrada = false; // Indica si se ha encontrado  el registro con los datos del tornillo

            while((sLinea=oLector.readLine()) != null){

                String[] aDatos = sLinea.split("\\|");

                //Busco el identificador del pedido en el registro de ORD
                if(aDatos[0].equals("ORD")){

                    // Compruebo que el registro tenga cinco campos y que el identificador no este vacio
                    if(aDatos.length == 5 && !aDatos[1].trim().isEmpty()){
                        sIdPedido = aDatos[1].trim();
                        bPedidoEncontrado = true;
                    }else{
                        System.err.println("Error: Linea ORD incorrecta");
                        System.exit(1);
                    }
                }
                
                //Busco los datos del pedido, en el registro LIN
                if(aDatos[0].equals("LIN")){
                     if(aDatos.length != 6 || !bPedidoEncontrado){
                        System.err.println("Error: Linea LIN incorrecta o sin identificador");
                        System.exit(1);
                    }

                    String sTipoTornillo = aDatos[2].trim();
                    int iCantidad = Integer.parseInt(aDatos[3].trim());
                    String sFormatoEmpaque = aDatos[4].trim();

                    //Compruebo que los datos del pedido sean correctos
                    if(iCantidad > 0 && (sTipoTornillo.equals("T1") || sTipoTornillo.equals("T2") || sTipoTornillo.equals("T3")) && (sFormatoEmpaque.equals("D") || sFormatoEmpaque.equals("P") || sFormatoEmpaque.equals("C"))){

                        System.out.println(sIdPedido + ";" + sTipoTornillo + ";" + iCantidad + ";" + sFormatoEmpaque);
                        bLineaEncontrada = true;

                    }else{
                        System.err.println("Error: Datos del pedido incorrectos");
                        System.exit(1);
                    }
                }
            }
                    
            //Compruebo que se hayan encontrado los datos y identificador del pedido
            if(!bPedidoEncontrado || !bLineaEncontrada){
                System.err.println("Error: No se ha encontrado el pedido");
                System.exit(1);                           
            }

        } catch (IOException e) {// Excepcion por si ocurre algun problema al abrir/leer el archivo
            System.err.println("Error al leer el archivo");
            System.exit(1);
        }

    }
}