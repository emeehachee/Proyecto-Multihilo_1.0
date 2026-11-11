import java.util.ArrayList;

/**
 * Oscar Martin Hernandez
 * 24/10/2026
 * Modulo: PSP
 * Clase Planificador de produccion
 * Esta clase distribuye los pedidos entre las maquinas segun el tipo de tornillo solicitado
 * distribuye los pedidos
 */

//Clase que extiende de Thread para que sus objetos se ejecuten en hilos independientes
public class CPlanificadorProduccion extends Thread {

    private final ArrayList<CPedido> aPedidos; // Lista de pedidos que el planificador ha recibido y todavia no ha repartido
    private final CMaquina oMaquinaT1;  // Referencia a la maquina encargada de fabricar tornillos T1
    private final CMaquina oMaquinaT2;  // Referencia a la maquina encargada de fabricar tornillos T2
    private final CMaquina oMaquinaT3;  // Referencia a la maquina encargada de fabricar tornillos T3
    private boolean bEjecutando = true; // Indica si el planificador debe seguir funcionando y aceptando pedidos

    public CPlanificadorProduccion(CMaquina oMaquinaT1, CMaquina oMaquinaT2, CMaquina oMaquinaT3) {
        aPedidos= new ArrayList<>();
        this.oMaquinaT1 = oMaquinaT1;
        this.oMaquinaT2 = oMaquinaT2;
        this.oMaquinaT3 = oMaquinaT3;
    }

    // Metodo sincronizado que recibe un pedido y lo guarda en la lista
    // oPedido: es el pedido que envia el coordinador
    public synchronized void agregarPedido(CPedido oPedido){
        aPedidos.add(oPedido);

        // Despierta al planificador si estaba esperando a que llegara un pedido
        notifyAll();
    }

    // Indica que no se recibiran mas pedidos
    public synchronized void detener() {
        bEjecutando = false;

        // Despierta al planificador por si estaba esperando pedidos
        notifyAll();
    }

        // Metodo run: contiene el trabajo que realiza el hilo planificador
    @Override
    public void run() {

        // Mantiene el planificador funcionando hasta que no queden pedidos y se haya solicitado su parada
        while(true){

            // Almacenara el pedido que se va a repartir.
            CPedido oPedido;

            // Protege el acceso a la lista y al estado del planificador
            // This: hace referencia a la instancia (el objeto especifico) que esta ejecutando ese codigo en ese momento
            synchronized (this) {
                // Si no hay pedidos y todavia se pueden recibir, espera
                while(aPedidos.isEmpty()  && bEjecutando){
                    try {
                        // Libera el bloqueo y espera a que llegue un pedido o se solicite la parada
                        wait();
                    } catch (InterruptedException e) {
                        // Solicita la interrupcion del hilo actual si se ha encontrado algun error
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Si no quedan pedidos y se ha solicitado detenerlo
                if (aPedidos.isEmpty() && !bEjecutando) {
                    break;
                }

                // Recoge y elimina el primer pedido de la lista
                oPedido=aPedidos.remove(0);
            }

            System.out.println("El planificador a recibido el pedido: "+ oPedido);

            // Compruebo el tipo de tornillo y envia el pedido a la maquina correspondiente
            if(oPedido.getsTipo().equals("T1")) {
                oMaquinaT1.agregarPedido(oPedido);
            }else if(oPedido.getsTipo().equals("T2")) {
                oMaquinaT2.agregarPedido(oPedido);
            }else if(oPedido.getsTipo().equals("T3")) {
                oMaquinaT3.agregarPedido(oPedido);
            }
        }

        // Avisar a las maquinas de que no ya no llegaran mas pedidos
        oMaquinaT1.detener();
        oMaquinaT2.detener();
        oMaquinaT3.detener();

        System.out.println("Planificador detenido");
    }  

}