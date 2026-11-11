import java.util.LinkedList;
import java.util.Random;

/**
 * Oscar Martin Hernandez
 * 15/10/2026
 * Modulo: PSP
 * Clase Maquina
 * Esta clase representa una maquina en la fabrica de tornillos
 * productor de tornillos
 */

//Clase que extiende de Thread para que sus objetos se ejecuten en hilos independientes
public class CMaquina extends Thread{

    //final para que no se pueda modificar
    private final String sIdMaquina;    //id de la maquina
    private final String sTipoTornillo; //tipo de tornillo que fabrica la maquina(T1,T2 O T3)
    private final CBufferLimitado oBufferD; //buffer donde se almacen los tornillos fabricados tipo de empaquetado D
    private final CBufferLimitado oBufferP;  //buffer donde se almacen los tornillos fabricados tipo de empaquetado P  
    private final CBufferLimitado oBufferC; //buffer donde se almacen los tornillos fabricados tipo de empaquetado C
    private final CAlmacen oAlmacen; // Almacen del que la maquina retira la materia prima necesaria para fabricar tornillos
    private final LinkedList<CPedido> aPedidos; //lista de pedidos pendientes de fabricar
    private final Random oAleatorio = new Random(); //Permite generar tiempos de fabriacion aleatorios para lo que tarda la maquina en fabricara cada tornillo
    private static final boolean  MODO_TEST=false; //False modo normal: 10-1000ms por tornillo
                                                   //True modo pruebas, entre 1 y 10ms por tornillo
    private boolean bEjecutando = true; // Indica si la maquina tiene que funcionando y aceptando pedidos
    private final CEstadisticas oEstadisticas;  // Objeto para registrar las estadisticas de la fabrica


    public CMaquina(String sIdMaquina, String sTipoTornillo, CBufferLimitado oBufferD, CBufferLimitado oBufferP, CBufferLimitado oBufferC, CAlmacen oAlmacen, CEstadisticas oEstadisticas) {
        this.sIdMaquina = sIdMaquina;
        this.sTipoTornillo = sTipoTornillo;
        this.oBufferD = oBufferD;
        this.oBufferP = oBufferP;
        this.oBufferC = oBufferC;
        this.oAlmacen = oAlmacen;
        this.oEstadisticas = oEstadisticas;
        aPedidos = new LinkedList<>();
    }

    public String getsIdMaquina() {
        return sIdMaquina;
    }

    public String getsTipoTornillo() {
        return sTipoTornillo;
    }
    
    //Sin set ya que el atributo es final y no se puede modificar una vez creado el objeto

    // Metodo sincronizado que añade un pedido a la lista de pedidos pendientes
    // oPedido: el pedido que debe fabricar la maquina
    public synchronized void agregarPedido(CPedido oPedido){
        
        // Añado el pedido al final de la lista
        aPedidos.add(oPedido);

        // Despierta a los hilos que puedan estar esperando a que llegue un pedido
        notifyAll();
    }

    // Metodo sincronizado que avisar a la maquina de que no recibira mas pedidos
    public synchronized void detener() {

        // Cambia el estado para indicar que la maquina debe dejar de aceptar pedidos
        bEjecutando = false;

        // Despierta al hilo por si estaba esperando a recibir un pedido.
        notifyAll();
    }

    // Metodo sincronizado que recoge el siguiente pedido pendiente
    // Devuelve el pedido que debe fabricar o null si la maquina debe finalizar
    public synchronized CPedido recogerPedido(){

        // Mientras no haya pedidos y la maquina siga funcionando, espera
        while(aPedidos.isEmpty()  && bEjecutando){
            try {
                // Libera el bloqueo y espera a que llegue un pedido o se detenga la maquina
                wait();
            } catch (InterruptedException e) {
                // Solicita la interrupcion del hilo actual si se ha encontrado algun error
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // Si no quedan pedidos y la maquina ha sido detenida, devuelve null
        if (aPedidos.isEmpty() && !bEjecutando) {
            return null;
        }

        // Retira y devuelve el primer pedido de la lista
        return aPedidos.removeFirst();
    }

     // Metodo run: contiene el trabajo que realiza la maquina cuando se inicia el hilo
    @Override
    public void run() {
        try {
            // La maquina sigue trabajando mientras el hilo no sea interrumpido
            while(!Thread.currentThread().isInterrupted()){

                // Recogo el siguiente pedido pendiente
                CPedido oPedido = recogerPedido();
                
                //Si no quedan pedidos y la maquina debe terminar, finalizo el hilo
                if(oPedido==null){
                    System.out.println("Maquina " + sIdMaquina + " finalizada");
                    return;
                }

                // Comprueba que el pedido corresponde al tipo de tornillo que fabrica esta maquina
                if(!oPedido.getsTipo().equals(sTipoTornillo)){
                    continue;
                }

                 // Repito el proceso tantas veces como el numero de tornillos se hayan solicitado en el pedido
                for(int i =0; i<oPedido.getiCantidad();i++){
                    
                    //Retira una unida de materia prima por tornillo
                    //ademas sirve como comprobacion de seguridad si se queda sin materiaPrima
                    if(!oAlmacen.retirarMaterial(1)){
                        System.err.println("No hay materia prima suficiente para fabricar el pedido: " + oPedido.getsIdPedido());
                        break;
                    }

                    // Almacena el tiempo que tardara en fabricarse el tornillo actual
                    int iTiempoFabricacion;

                    if(MODO_TEST){
                        // del 1 al 10 gracias al +1 que desplaza el 0 al 9 al 1 al 10
                        iTiempoFabricacion=oAleatorio.nextInt(10)+1;
                    }else{
                        // del 10 al 1000 gracias al +10 que desplaza el 0 al 990 al 10 al 1000
                        iTiempoFabricacion=oAleatorio.nextInt(991)+10;
                    }
                    
                    // Simula el tiempo que tarda la maquina en fabricar el tornillo
                    Thread.sleep(iTiempoFabricacion);

                    // Crea un tornillo asociado al identificador del pedido y al tipo de la maquina
                    CTornillo oTornillo= new CTornillo(oPedido.getsIdPedido(), sTipoTornillo);      
                    
                    // Actualizo las estadisticas con el tornillo fabricado
                    oEstadisticas.sumarTornilloFabricado();

                    System.out.println("Maquina " + sIdMaquina + " : tornillo fabricado para el pedido " + oPedido.getsIdPedido() + " | Tipo: " + sTipoTornillo);
                    
                    //Envia el tornillo al bufer del formato del formato de empaquetado correspondiente
                    if(oPedido.getsFormatoEmpaquetado().equals("D")){
                        oBufferD.producir(oTornillo);
                    }else if(oPedido.getsFormatoEmpaquetado().equals("P")){
                        oBufferP.producir(oTornillo);     
                    }else if (oPedido.getsFormatoEmpaquetado().equals("C")) {
                        oBufferC.producir(oTornillo);     
                    }
                
                }

                //Avisar al consumidor de que este pedido ha terminado
                CTornillo oFinPedido = new CTornillo(
                    oPedido.getsIdPedido(),
                    sTipoTornillo,
                    true
                );

                // Envia la señal de fin al bufer correspondiente
                if(oPedido.getsFormatoEmpaquetado().equals("D")){
                    oBufferD.producir(oFinPedido);
                }else if(oPedido.getsFormatoEmpaquetado().equals("P")){
                    oBufferP.producir(oFinPedido);     
                }else if (oPedido.getsFormatoEmpaquetado().equals("C")) {
                    oBufferC.producir(oFinPedido);     
                }
            }       

            
        } catch (InterruptedException e) {
            // Solicita la interrupcion del hilo actual si se ha encontrado algun error
            Thread.currentThread().interrupt();
        }
    }
    

    @Override
    public String toString() {
        return "CMaquina [sIdMaquina=" + sIdMaquina + ", sTipoTornillo=" + sTipoTornillo + "]";
    }
 
}