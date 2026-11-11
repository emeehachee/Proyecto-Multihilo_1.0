import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Oscar Martin Hernandez
 * 20/10/2026
 * Modulo: PSP
 * Clase CLector3
 * Esta clase representa un lector de pedidos de la fabrica
 */

/*
01A28901234CMI-2026000320261018CONSTRUC METALICAS IBERICAS  PLANTA MADRID SUR   
02CMI-20260003T2000200CTORNILLO METRICO ZINCADO   POLIGONO INDUSTRIAL PINTO     
 */

public class CLector3 {

    // Metodo principal que recibe la ruta del archivo y procesa su contenido
    public static void main(String[] args) {

        // Compruebo que se haya indicado exactamente un argumento: la ruta del archivo
        if(args.length != 1){
            System.err.println("Error: Debes indicar la ruta del archivo");
            System.exit(1); // Termina el proceso Java indicando que ha habido un error
        }

        try(BufferedReader oLector = new BufferedReader(new FileReader(args[0]))){
            
            String sLinea1= oLector.readLine();
            String sLinea2= oLector.readLine();

            if(sLinea1== null || sLinea2==null  ||sLinea1.length()!=80 || sLinea2.length()!=80 || oLector.readLine()!=null){
                System.err.println("Error: El archivo debe tener exactamente dos lineas de 80 caracteres");
                System.exit(1);
            } 

            String sCodReg1 = sLinea1.substring(0,2).trim();
            String sCif = sLinea1.substring(2,11).trim();
            String sIdPedido = sLinea1.substring(11,23).trim();

            String sCodReg2 = sLinea2.substring(0,2);
            String sIdPedidoVinc = sLinea2.substring(2,14).trim();
            String sTipoTornillo = sLinea2.substring(14,16).trim();
            int iCantidad = Integer.parseInt(sLinea2.substring(16,22).trim());
            String sFormatoEmpaque = sLinea2.substring(22,23).trim();
            
            // Compruebo que el primer registro sea 01 y el segundo sea 02
            if(!sCodReg1.equals("01") || !sCodReg2.equals("02")){
                System.err.println("Error: Codigo de registro incorrecto");
                System.exit(1);
            }

            // Compruebo que el CIF del proveedor sea el esperado
            if(!sCif.equals("A28901234")){
                System.err.println("Error: CIF de la empresa incorrecto");
                System.exit(1);
            }    

            // Compruebo que el identificador del pedido coincida en los dos registros
            if(!sIdPedido.equals(sIdPedidoVinc)){
                System.err.println("Error: Los identificadores del pedido no coinciden");
                System.exit(1);
            }
           
            // Compruebo que la cantidad sea positiva, que el tipo sea T1, T2 o T3 y que el formato de empaquetado sea D, P o C
            if(iCantidad >0 && (sTipoTornillo.equals("T1") || sTipoTornillo.equals("T2") || sTipoTornillo.equals("T3")) && (sFormatoEmpaque.equals("D") || sFormatoEmpaque.equals("P") || sFormatoEmpaque.equals("C"))){
                System.out.println(sIdPedido + ";" + sTipoTornillo + ";" + iCantidad + ";" +sFormatoEmpaque);
            }else{
                System.err.println("Error: formato de lienas incorrecto ");
                System.exit(1);
            }


        } catch (IOException e) { // Excepcion por si ocurre algun problema al abrir/leer el archivo
            System.err.println("Error al leer el archivo");
            System.exit(1);
        }
    }
}