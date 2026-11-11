import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Oscar Martin Hernandez
 * 19/10/2026
 * Modulo: PSP
 * Clase CLector1
 * Esta clase representa un lector de pedidos de la fabrica
 */

/*
======================================================================
ORDEN DE SUMINISTRO INDUSTRIAL - FERRETERIA INDUSTRIAL DEL DUERO S.L.
======================================================================
NIF CLIENTE  : B47120934
PEDIDO NUM   : FID-2026-0891
FECHA PEDIDO : 18/10/2026
HORA PEDIDO  : 10:20:45
DESTINO      : NAVE INDUSTRIAL VALLADOLID
----------------------------------------------------------------------
POS  REFERENCIA          TIPO  CANTIDAD   EMBALAJE   PLAZO_DIAS
----------------------------------------------------------------------
01   TORN-HEX-ALTA-RES   T3    40         P          03
======================================================================
ESTADO: APROBADO | CONTROL_CALIDAD: SI

 */

public class CLector2 {

    // Metodo principal que recibe la ruta del archivo y procesa su contenido
    public static void main(String[] args) {

        // Compruebo que se haya indicado exactamente un argumento: la ruta del archivo
        if(args.length != 1){
            System.err.println("Error: Debes indicar la ruta del archivo");
            System.exit(1); // Termina el proceso Java indicando que ha habido un error
        }

        try(BufferedReader oLector = new BufferedReader(new FileReader(args[0]))){
            
            String sLinea;
            String sIdPedido= ""; // Guarda el identificador del pedido encontrado en el archivo
            boolean bPedidoEncontrado = false;  // Indica si se ha encontrado el identificador del pedido

            while((sLinea=oLector.readLine()) != null){

                if(sLinea.contains("PEDIDO NUM")){
                    String[] aDatos = sLinea.split(":");

                    if(aDatos.length < 2 || aDatos[1].trim().isEmpty()){
                        System.err.println("Error: Identificador del pedido incorrecto");
                        System.exit(1);
                    }

                    sIdPedido = aDatos[1].trim();
                    bPedidoEncontrado = true;
                }

                //Busco la linea que contiene los datos del pedido
                if(sLinea.trim().startsWith("01")){

                    String[] aDatos = sLinea.trim().split("\\s+");

                    //Compruebo que exista el identificador y que la linea este completa (6 campos)
                    if(!bPedidoEncontrado || aDatos.length < 6){
                        System.err.println("Error: Linea de pedido incompleta o sin identificador: " + sLinea);
                        System.exit(1);
                    }

                    String sTipoTornillo = aDatos[2];
                    int iCantidad = Integer.parseInt(aDatos[3]);
                    String sFormatoEmpaque = aDatos[4];

                    //Compruebo que los datos del pedido sean correctos
                    if(iCantidad <= 0 ||!(sTipoTornillo.equals("T1") || sTipoTornillo.equals("T2") || sTipoTornillo.equals("T3")) ||!(sFormatoEmpaque.equals("D") || sFormatoEmpaque.equals("P") || sFormatoEmpaque.equals("C"))){

                        System.err.println("Error: Datos del pedido incorrectos: " + sLinea);
                        System.exit(1);
                    }

                    //Muestro el pedido
                    System.out.println(sIdPedido + ";" + sTipoTornillo + ";" + iCantidad + ";" + sFormatoEmpaque);
                }
            }

            //Compruebo que se haya encontrado un pedido
            if(!bPedidoEncontrado){
                System.err.println("Error: No se ha encontrado el identificador del pedido");
                System.exit(1);
            }

        } catch (IOException e) { // Excepcion por si ocurre algun problema al abrir/leer el archivo
            System.err.println("Error al leer el archivo");
            System.exit(1);
        }

    }
}