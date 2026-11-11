import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Oscar Martin Hernandez
 * 22/10/2026
 * Modulo: PSP
 * Clase Proceso coordinador
 * Esta clase coordina la lectura de los pedidos de los proveedores
 * recoge y procesa los archivos
 */

public class CProcesoCoordinador extends Thread{

    private final Path oRutaProyecto; //Ruta absulta del directorio desde el que se ejecuta el proyecto
    private final String sClasspath;  //Classpath (es un parametro que le indica al compilador y a la Maquina Virtual de Java dónde deben buscar las clases, paquetes y archivos de recursos) del proyecto utilizado para ejectuar los programas lectores
    private final String sRutaJava;   //Ruta del ejectuable Java del S.O.
    private final CPlanificadorProduccion oPlanificador; //Referencia al planificador al que se envian los pedidos validos
    private volatile boolean bEjecutando = true; //Indica si el coordinador debe continuar vigilando los directorios
    private final CEstadisticas oEstadisticas; // Objeto para registrar las estadisticas de la fabrica


    /**
     * Constructor: inicia las rutas necesarias y guarda la referencia
     * al planificador de produccion.
     * @param oPlanificador
     */
    public CProcesoCoordinador(CPlanificadorProduccion oPlanificador,CEstadisticas oEstadisticas){
        
        //// Obtiene el directorio desde el que se ejecuta el proyecto
        oRutaProyecto= Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
        // Obtiene las rutas de las clases necesarias para ejecutar los lectores
        sClasspath= System.getProperty("java.class.path");
        
        // Obtiene el nombre del sistema operativo en minusculas
        String sSistema = System.getProperty("os.name").toLowerCase();

        //Comprueba si el sistema operativo es Windows
        if (sSistema.contains("win")) {
            //En Windows, el ejecutable Java se llama java.exe
            sRutaJava = Paths.get(System.getProperty("java.home"), "bin", "java.exe").toString();
        } else {
            //En otros sistemas, el ejecutable Java se llama java
            sRutaJava = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        }

        // Guarda el planificador recibido para poder enviarle los pedidos
        this.oPlanificador = oPlanificador;
        this.oEstadisticas = oEstadisticas;
    }

    /**
     * Metodo para solicitar la detencion del coordinador
     * Cambia el indicador que controla el bucle de vigilancia
     */
    public void detener() {
        bEjecutando = false;
    }

    /*
     * Metodo run(): contiene el trabajo que realiza el hilo coordinador
     * cuando se inicia mediante start()
     */
    @Override
    public void run() {

        try {
            //Crea los directorios de los seis proveedores si no existen
            crearDirectoriosProveedores();

            //Comienza a vigilar los directorios y procesar los ficheros
            mirarDirectorios();

            //Cuando termina la vigilancia, solicita detener el planificador
            oPlanificador.detener();

        } catch (IOException e) {
            //Muestra un mensaje si se produce un error creando los directorios
            System.err.println("Error al crear los directorios de proveedores: " + e.getMessage());
        }
    }

    /*
     * Metodo que comprueba periodicamente los directorios de los proveedores
     * para detectar nuevos ficheros de pedidos
     */
    public void mirarDirectorios(){
        System.out.println("Coordinador iniciado ");

        try {
            //El coordinador continua trabajando mientras este activo
            while(bEjecutando){
                
                //Revisar los seis directorios de proveedores
                for(int i=1; i<=6;i++){
                    // Obtiene la ruta del directorio del proveedor actual
                    Path oCarpeta= oRutaProyecto.resolve("datos/proveedor"+i);

                    //Obtiene los ficheros que contiene ese directorio
                    File[] aFicheros = oCarpeta.toFile().listFiles();

                    // Compruebo que se ha podido obtener la lista de ficheros
                    if(aFicheros!=null){
                        //Recorre todos los ficheros encontrados
                        for(File oFichero: aFicheros){
                            //Compruebo que el elemento encontrado es un fichero
                            if(oFichero.isFile()){
                                 //Procesa el fichero utilizando el lector del proveedor
                                procesarFichero(oFichero.toPath(),i);
                            }
                        }
                    }
                }

                //Si se ha solicitado detenerlo, sale del bucle
                if(!bEjecutando) {
                    break;
                }

                // Espera un segundo antes de volver a revisar los directorios
                Thread.sleep(1000);
            }

            System.out.println("Coordinador detenido");

        }catch(InterruptedException e) {
            // Recupera el estado de interrupcion del hilo
            Thread.currentThread().interrupt();
            System.out.println("Cooridnador detenido");
        }catch(Exception ex){
            //Muestra cualquier otro error producido durante la vigilancia
            System.err.println(ex.getMessage());
        }
    }

    /*
     * Metodo que ejecuta el lector correspondiente al proveedor y recoge
     * su salida para convertirla en pedidos que recibe el planificador
     */
    private void procesarFichero(Path oFichero, int iProveedor){

        //Construye el nombre de la clase lectora correspondiente
        String sLector="CLector"+ iProveedor;

        System.out.println("Archivo encontrado: "+ oFichero.getFileName());
        
        try {
            // Prepara el proceso externo que ejecutara el lector correspondiente
            ProcessBuilder oConstructor=new ProcessBuilder(sRutaJava, "-cp", sClasspath, sLector, oFichero.toAbsolutePath().toString());

            // Establece el directorio de trabajo del proceso.
            oConstructor.directory(oRutaProyecto.toFile());

            // Inicia el proceso externo del lector
            Process oProceso = oConstructor.start();

            // Almacena la salida normal del lector
            StringBuilder sSalida = new StringBuilder();

            // Almacena los mensajes de error del lector
            StringBuilder sErrores = new StringBuilder();

            // Crea un hilo para leer la salida normal del proceso
            Thread oHiloSalida= leerFlujo(oProceso.getInputStream(),sSalida);
            
            // Crea otro hilo para leer los errores del proceso
            Thread oHiloErrores = leerFlujo(oProceso.getErrorStream(),sErrores);

            // Inicia la lectura de la salida normal
            oHiloSalida.start();

            // Inicia la lectura de los errores
            oHiloErrores.start();

            // Espera a que termine el proceso lector y obtiene su codigo de salida
            int iCodigoSalida = oProceso.waitFor();
            
            // Espera a que termine el hilo que recoge la salida normal
            oHiloSalida.join();

            // Espera a que termine el hilo que recoge los errores
            oHiloErrores.join();

            // Compruebo que el lector ha terminado correctamente y ha generado salida
            if (iCodigoSalida == 0 && sSalida.toString().trim().length() > 0) {
                // Separa la salida en lineas
                String[] aLineas = sSalida.toString().trim().split("\\R");

                // Lista donde se almacenan los pedidos obtenidos del lector
                ArrayList<CPedido> aPedidos = new ArrayList<>();
                
                // Recorre cada linea generada por el lector
                for (String sLinea : aLineas) {
                    // Separa los cuatro datos del pedido utilizando el punto y coma
                    //El -1 hace que split() conserve tambien los campos vacíos del final
                    String[] aDatos = sLinea.split(";",-1);
                    
                     // Comprueba que la linea contiene los cuatro campos y que ninguno esta vacio
                    if (aDatos.length != 4 || aDatos[0].trim().isEmpty() || aDatos[1].trim().isEmpty() || aDatos[2].trim().isEmpty() || aDatos[3].trim().isEmpty()) {

                        throw new IOException("Formato de salida incorrecto del lector");
                    }

                    // Recoge el identificador del pedido
                    String sIdPedido = aDatos[0].trim();

                    // Recoge el tipo de tornillo solicitado
                    String sTipo = aDatos[1].trim();

                    // Recoge la cantidad de tornillos solicitada
                    int iCantidad = Integer.parseInt(aDatos[2].trim());
                    
                    // Recoge el formato de empaquetado solicitado
                    String sFormato = aDatos[3].trim();

                    // Creo el objeto pedido con los datos obtenidos
                    CPedido oPedido = new CPedido(sIdPedido, sTipo, iCantidad, sFormato);

                    // Añado el pedido a la lista de pedidos
                    aPedidos.add(oPedido);
                }

                // Recorro los pedidos obtenidos para enviarlos al planificador
                for (CPedido oPedido : aPedidos) {
                    // Añado el pedido a la cola del planificador
                    oPlanificador.agregarPedido(oPedido);
                    System.out.println("Pedido enviado al planificador: " + oPedido);
                }

                // Muevo el fichero a la carpeta de pedidos procesados
                moverFichero(oFichero, "procesados");

                // Actualizo las estadisticas con el archivo procesado
                oEstadisticas.sumarArchivoProcesado();

            }else{
                 // Informo de que el lector no ha procesado correctamente el fichero
                System.err.println("Error al procesar "+ oFichero.getFileName());
                
                // Muestro los errores devueltos por el lector
                System.err.println(sErrores.toString());

                // Muevo el fichero a la carpeta de errores
                moverFichero(oFichero, "errores");

                // Actualizo las estadisticas con el archivo rechazado
                oEstadisticas.sumarArchivoRechazado();
                oEstadisticas.sumarError();
            }        
        } catch(InterruptedException e) {
            // Recupera el estado de interrupcion del hilo
            Thread.currentThread().interrupt();
        }catch(Exception e){
            // Muestro el nombre del fichero y el error producido
            System.err.println("Error al procesar " + oFichero.getFileName());
            System.err.println(e.getMessage());

            // Actualizo las estadisticas con el error producido
            oEstadisticas.sumarError();

            try {
                // Si se produce un error, mueve el fichero a la carpeta de errores
                moverFichero(oFichero, "errores");

                oEstadisticas.sumarArchivoRechazado();
            }catch (IOException ex) {
                // Muestro el error si tampoco se puede mover el fichero
                System.err.println(ex.getMessage());
            }
        }
    }

    /**
     * Metodo que crea un hilo encargado de leer un flujo de entrada
     * y guardar todo su contenido en el StringBuilder recibido
     */
    private Thread leerFlujo(final InputStream oFlujo, final StringBuilder sContenido){

        // Crea el hilo que realizara la lectura del flujo
        Thread oHilo = new Thread(new Runnable() {
            @Override
            public void run() {

                try (BufferedReader oLector = new BufferedReader(new InputStreamReader(oFlujo))) {
                    
                    // Variable donde se almacena cada linea leida
                    String sLinea;

                    // Lee el flujo hasta que no queden mas lineas
                    while((sLinea = oLector.readLine())!=null){
                        // Añade la linea leida al contenido almacenado
                        sContenido.append((sLinea)).append(System.lineSeparator());
                    }

                } catch (IOException e) {
                    // Guarda el error producido durante la lectura
                    sContenido.append("Error leyendo el flujo: ").append(e.getMessage());
                }
            }        
        });

        // Devuelve el hilo creado para que pueda iniciarse desde otro metodo
        return oHilo;
    }

    /*
     * Metodo que mueve un fichero desde su directorio de proveedor
     * hasta la carpeta de destino indicada
     */
    private void moverFichero(Path oOrigen, String sCarpetaDestino) throws IOException{

        // Obtiene la ruta de la carpeta de destino
        Path oDestino = oRutaProyecto.resolve(sCarpetaDestino);

        // Crea la carpeta de destino si todavia no existe
        Files.createDirectories(oDestino);

        // Construye la ruta completa del fichero en su nueva ubicacion
        Path oFicheroDestino = oDestino.resolve(oOrigen.getFileName());

        // Mueve el fichero utilizando una operacion atomica
        Files.move(oOrigen, oFicheroDestino, StandardCopyOption.ATOMIC_MOVE);

        System.out.println("Archivo movido a : "+ sCarpetaDestino);
    }

    /*
     * Metodo que crea los seis directorios de proveedores
     * si no existen previamente
     */
    private void crearDirectoriosProveedores() throws IOException {

        // Recorre los seis proveedores
        for (int i = 1; i <= 6; i++) {
            // Construye la ruta del directorio del proveedor actual
            Path oCarpeta = oRutaProyecto.resolve("datos/proveedor" + i);
            
            // Crea el directorio, incluyendo los directorios superiores necesarios
            Files.createDirectories(oCarpeta);
        }
}

}