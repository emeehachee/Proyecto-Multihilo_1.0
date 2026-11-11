import java.util.ArrayList;

/**
 * Oscar Martin Hernandez
 * 16/10/2026
 * Modulo: PSP
 * Clase Buffer limitado
 * Esta clase gestiona un buffer limitado para intercambiar datos entre hilos productores y consumidores
 * comunica productores y consumidores
 */

public class CBufferLimitado{

    //Final para que no se pueda cambiar la referencia del buffer ni la capacidad del buffer
    private final ArrayList<CTornillo> aBuffer; // Buffer que almacena los tornillos( Lista de tornillos) fabricados(pendientes de empaquetar)
    
    //Final porque la capacidad se establece al crear el buffer
    private final int iCapacidadBuffer; // Capacidad maxima de tornilos que puede almacenar el buffer

    // iCapacidadBuffer: numero maximo de tornillos que puede contener
    public CBufferLimitado( int iCapacidadBuffer) {
        this.aBuffer = new ArrayList<>();
        this.iCapacidadBuffer = iCapacidadBuffer;
    }

    //Solo get de la capcidad del buffer y no del arrary de bufer para que otra clase no pueda modificar y acceder al buffer
    public int getiCapacidadBuffer() {
        return iCapacidadBuffer;
    }

    // Metodo sincronizado que introduce un tornillo en el buffer
    // oElemento: tornillo que el productor quiere introducir
    // Puede lanzar InterruptedException si el hilo es interrumpido mientras espera
    public synchronized void producir(CTornillo oElemento) throws InterruptedException {
        
        // Compruebo si el buffer esta lleno
        // Mientras no haya espacio, el productor espera
        while (aBuffer.size() >= iCapacidadBuffer){
            wait(); // // El hilo productor queda esperando hasta que haya espacio disponible
        }

        aBuffer.add(oElemento); // Agrego el objeto tornillo al final del buffer

        notifyAll(); // Notifico los hilos consumidores que esperan porque el buufer estaba vacio, que hay un nuevo elemento disponible

    }

    /**
     * Metodo sincronizado que obtiene y elimina un tornillo del buffer
     * Devuelve el primer tornillo disponible
     */
    public synchronized CTornillo consumir() throws InterruptedException {
        
        // Compruebo si el buffer esta vacio
        // Mientras no haya tornillos, el consumidor espera
        while (aBuffer.isEmpty()){
            wait(); // Espera si el buffer está vacio (se paran los hilos consumidores(el hilo que se este ejecutando) hasta que haya un elemento disponible en el buffer)
        }

        CTornillo oElemento = aBuffer.remove(0); // Obtengo y elimina el primer elemento del buffer(FIFO)

        notifyAll(); // Notifica a los hilos productores que esperan porque el buffer estaba lleno, que hay espacio disponible en el buffer

        return oElemento;  //Se devuelve un objeto tornillo porque el empaquetador necesita recibir el tornillo para poder trabajar con el
    }
    
}
