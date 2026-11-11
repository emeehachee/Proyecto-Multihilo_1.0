
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Oscar Martin Hernandez
 * 26/10/2026
 * Modulo: PSP
 * Clase Consumidor
 * Esta clase representa una consumidor de la fabrica que se encarga de empaquetar los tornillos
 * consumidor de tornillos
 */

public class CConsumidor extends Thread{

    private final CBufferLimitado oBuffer;// Buffer del que el consumidor recoge los tornillos fabricados
    private final CEmpaquetador oEmpaquetador; // Empaquetador que contiene el identificador y la capacidad de cada paquete
    
    // La clave es el identificador del pedido y el valor es la lista de tornillo que se estan acumulando para ese pedido
    private final HashMap<String, ArrayList<CTornillo>> aPedidos;// Guarda los pedidos que esta gestionando el consumidor

    private final Random oAleatorio = new Random();// Generador de numeros aleatorios para simular el tiempo de empaquetado

    // Como es static y final, es un valor compartido por la clase y no se puede modificar
    private static final boolean MODO_TEST = false; // Indica si se esta ejecutando el programa en modo de pruebas

    private final CEstadisticas oEstadisticas; // Objeto para registrar las estadisticas de la fabrica

    // Constructor que recibe el buffer del que recoger tornillos y el empaquetador
    public CConsumidor(CBufferLimitado oBuffer, CEmpaquetador oEmpaquetador,CEstadisticas oEstadisticas) {
        this.oBuffer = oBuffer;
        this.oEmpaquetador = oEmpaquetador;
        this.oEstadisticas = oEstadisticas;
        this.aPedidos = new HashMap<>();
    }

    // Metodo run: contiene el trabajo que realiza el hilo consumidor cuando se inicia
    @Override
    public void run() {
        try {
            // El consumidor sigue trabajando mientras no se interrumpa el hilo
            while (!Thread.currentThread().isInterrupted()) {

                // Recogo un tornillo del buffer
                CTornillo oTornillo = oBuffer.consumir();

                // Compruebo si se ha recibido la señal del Poison Pill para finalizar
                if (oTornillo.isPoisonPill()) {
                    System.out.println("Empaquetador "+ oEmpaquetador.getsIdEmpaquetador()+ " finalizado");
                    break;
                }

                // Recogo el identificador del pedido al que pertenece el tornillo
                String sIdPedido = oTornillo.getIdPedido();

                // Comprobuebo si el tornillo recibico indica si ha terminado el pedido
                if (oTornillo.isFinPedido()) {

                    // Actualizo las estadisticas con el pedido terminado
                    oEstadisticas.sumarPedidoTerminado();
                    
                     // Comprobuebo si el pedido esta guardado en el mapa
                    if (aPedidos.containsKey(sIdPedido)) {

                        // Recupera la lista de tornillos que quedan pendientes de empaquetar para ese pedido
                        ArrayList<CTornillo> aTornillos = aPedidos.get(sIdPedido);

                        // Cerrar el paquete final si quedan tornillos aunque no este completo
                        if (!aTornillos.isEmpty()) {
                            System.out.println("Pedido " + sIdPedido+ " : paquete final cerrado por empaquetador: "+ oEmpaquetador.getsIdEmpaquetador()+ " con " + aTornillos.size()+ " tornillos");
                            
                            // Actualizo las estadisticas con el paquete final cerrado
                            oEstadisticas.sumarPaqueteCerrado();
                        }
                        
                         // Elimino el pedido del mapa porque ya se ha terminado de empaquetar
                        aPedidos.remove(sIdPedido);
                    }

                } else {

                    // // Variable que almacena el tiempo que tarda en empaquetarse un tornillo, es decir simular el tiempo de empaquetado
                    int iTiempoEmpaquetado;

                    // Miro que modo se ha elegido
                    // En modo de pruebas se utiliza un tiempo de espera mas corto
                    if (MODO_TEST) {
                        iTiempoEmpaquetado = oAleatorio.nextInt(10) + 1;
                    } else {
                        // En funcionamiento normal se genera un tiempo aleatorio entre 10 y 1000 ms
                        iTiempoEmpaquetado = oAleatorio.nextInt(991) + 10;
                    }

                    // Simula el tiempo que tarda el empaquetador en trabajar
                    Thread.sleep(iTiempoEmpaquetado);

                    // Si el pedido todavia no esta registrado, creo una lista para sus tornillos
                    if (!aPedidos.containsKey(sIdPedido)) {
                        aPedidos.put(sIdPedido, new ArrayList<>());
                    }

                    // Recupero la lista de tornillos correspondiente al pedido actual
                    ArrayList<CTornillo> aTornillos =aPedidos.get(sIdPedido);

                    aTornillos.add(oTornillo);

                    // Cierro el paquete cuando alcanza su capacidad maxima
                    if (aTornillos.size() >= oEmpaquetador.getiCantidadTornillos()) {
                        
                        //Muestro el paquete cerrado
                        System.out.println("Pedido " + sIdPedido+ " : paquete cerrado por el empaquetador: "+ oEmpaquetador.getsIdEmpaquetador()+ " con " + aTornillos.size()+ " tornillos");
                        
                        // Actualizo las estadisticas con el paquete final cerrado
                        oEstadisticas.sumarPaqueteCerrado();

                        // Elimino el pedido del mapa porque este paquete ya se ha cerrado
                        aPedidos.remove(sIdPedido);
                    }
                }
            }
            
        } catch (InterruptedException e) {
            // Solicita la interrupcion del hilo actual si se ha encontrado algun error
            Thread.currentThread().interrupt();
        }
    }
}
