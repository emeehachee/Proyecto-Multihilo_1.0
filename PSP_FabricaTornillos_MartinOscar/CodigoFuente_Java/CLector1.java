import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Oscar Martin Hernandez
 * 19/10/2026
 * Modulo: PSP
 * Clase CLector1
 * Esta clase representa un lector de pedidod de la fabrica
 */

/* 
# ==============================================================================
# PROVEEDOR 1: Automocion del Cantabrico S.A.
# CIF: A33012948 | ERP: SAP/Navision Export
# ==============================================================================
# NUM_PEDIDO_ORIGEN: AC-2026-4410
# FECHA_ORDEN: 2026-10-18
# HORA_ORDEN: 10:15:30
# CENTRO_ENTREGA: PLANTA ENSAMBLAJE GIJON
LINEA;COD_PEDIDO;FECHA;TIPO_TORNILLO;CANTIDAD;FORMATO_EMPAQUE;REF_INTERNA;PRECIO_UNITARIO
1;AC-2026-4410;2026-10-18;T1;120;D;PERNO-MET-M4;0.18
*/

public class CLector1 {

    // Metodo principal que recibe la ruta del archivo y procesa su contenido
    public static void main(String[] args) {

        // Compruebo que se haya indicado exactamente un argumento: la ruta del archivo
        if(args.length != 1){
            System.err.println("Error: Debes indicar la ruta del archivo");
            System.exit(1); // Termina el proceso Java indicando que ha habido un error
        }

        //Flujo para poder leer el archivo
        try(BufferedReader oLector = new BufferedReader(new FileReader(args[0]))){
            
            String sLinea;

            while((sLinea = oLector.readLine()) !=null){

                // Ignoro las lineas vacias, los metadatos (empiezan por #) y la cabecera de las columnas
                if (sLinea.trim().isEmpty() || sLinea.trim().startsWith("#") || sLinea.trim().startsWith("LINEA")) {
                    continue;
                }

                String[] aDatos = sLinea.split(";");

                //Compruebo que la linea tenga los 8 campos
                if(aDatos.length != 8){
                    System.err.println("Error: Numero de campos incorrecto: " + sLinea);
                    System.exit(1);
                }

                
                String sIdPedido= aDatos[1];
                String sTipoTornillo= aDatos[3];
                int iCantidad = Integer.parseInt(aDatos[4]);
                String sFormatoEmpaque = aDatos[5];
                
                //Compruebo que los datos del pedido sean correctos
                if(iCantidad >0 && (sTipoTornillo.equals("T1") || sTipoTornillo.equals("T2") || sTipoTornillo.equals("T3")) && (sFormatoEmpaque.equals("D") || sFormatoEmpaque.equals("P") || sFormatoEmpaque.equals("C"))){
                    System.out.println(sIdPedido + ";" + sTipoTornillo + ";" + iCantidad + ";" +sFormatoEmpaque);
                }else{
                    System.err.println("Error: formato de lienas incorrecto: " + sLinea);
                    System.exit(1);
                }
            }
        } catch (IOException e) { // Excepcion por si ocurre algun problema al abrir/leer el archivo
            System.err.println("Error al leer el archivo");
            System.exit(1);
        }

    }
}