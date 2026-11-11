import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Oscar Martin Hernandez
 * 22/10/2026
 * Modulo: PSP
 * Clase CLector5
 * Esta clase representa un lector de pedidos de la fabrica
 */

/*
[CABECERA_DOCUMENTO]
cif_empresa=E46991823
razon_social=Electro-Mecanica Levante C.B.
fecha_operacion=2026-10-18
hora_registro=10:35:10

[DATOS_PEDIDO]
id_pedido_cliente=EML-2026-0554
tipo_tornillo=T2
cantidad_unidades=36
embalaje_requerido=D
centro_entrega=ALMACEN_VALENCIA_SUR
 */

public class CLector5 {

    // Metodo principal que recibe la ruta del archivo y procesa su contenido
    public static void main(String[] args) {

        // Compruebo que se haya indicado exactamente un argumento: la ruta del archivo
        if(args.length != 1){
            System.err.println("Error: Debes indicar la ruta del archivo");
            System.exit(1); // Termina el proceso Java indicando que ha habido un error
        }

        //Flujo de lectura del archivo
        try(BufferedReader oLector = new BufferedReader(new FileReader(args[0]))){
            
            String sLinea;
            String sIdPedido= ""; // Guarda el identificador del pedido
            String sTipoTornillo=""; // Guarda el tipo de tornillo que se ha pedido
            String sCantidad= ""; // Guarda la cantidad como texto hasta convertirla posteriormente a entero
            String sFormatoEmpaque=""; // Guarda el formato de empaquetado que se ha pedido

            while((sLinea=oLector.readLine()) != null){
                
                //Busco los datos del pedido
                if(sLinea.contains("=")){
                    String[] aDatos = sLinea.split("=");

                    if(aDatos[0].equals("id_pedido_cliente")){
                        sIdPedido=aDatos[1];
                    }

                    if(aDatos[0].equals("tipo_tornillo")){
                        sTipoTornillo=aDatos[1];
                    }

                    if(aDatos[0].equals("cantidad_unidades")){
                        sCantidad=aDatos[1];
                    }

                    if(aDatos[0].equals("embalaje_requerido")){
                        sFormatoEmpaque=aDatos[1];
                    }
                }
            }

            //Compruebo que se hayan encontrado todos los datos
            if(sIdPedido.isEmpty() || sTipoTornillo.isEmpty() || sCantidad.isEmpty() || sFormatoEmpaque.isEmpty()){
                System.err.println("Error: Faltan datos del pedido");
                System.exit(1);
            }

            int iCantidad = Integer.parseInt(sCantidad);

            //Compruebo que los datos del pedido sean correctos
            if(iCantidad >0 && (sTipoTornillo.equals("T1") || sTipoTornillo.equals("T2") || sTipoTornillo.equals("T3")) && (sFormatoEmpaque.equals("D") || sFormatoEmpaque.equals("P") || sFormatoEmpaque.equals("C"))){
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