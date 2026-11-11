import java.util.Scanner;

/**
 * Oscar Martin Hernandez
 * 16/10/2026
 * Modulo: PSP
 * Clase Principal
 * Esta clase crea y conecta los elementos de la fabrica de tornillos
 * conecta todos los elementos
 */

public class CPrincipal{

    public static void main(String[] args){

        // Creo el almacen con una cantidad muy grande de materia prima
        CAlmacen oAlmacen = new CAlmacen((10000000));

        // Creo el objeto de estadisticas, que se encarga de mostar las estadisitcas
        CEstadisticas oEstadisticas = new CEstadisticas();

        // Creo los tres buffers de empaquetado
        // Capacidad maxima configurable: 50
        CBufferLimitado oBufferD = new CBufferLimitado(50);
        CBufferLimitado oBufferP = new CBufferLimitado(50);
        CBufferLimitado oBufferC = new CBufferLimitado(50);
        
        // Creo los empaquetadores
        // D: paquetes de 12 tornillos
        // P: paquetes de 2 tornillos
        // C: paquetes de 100 tornillos
        CEmpaquetador oEmpaquetadorD = new CEmpaquetador(("D"), 12);
        CEmpaquetador oEmpaquetadorP = new CEmpaquetador(("P"), 2);
        CEmpaquetador oEmpaquetadorC = new CEmpaquetador(("C"), 100);

        // Creo los consumidores
        // Cada consumidor recoge tornillos de su propio buffer y utiliza su empaquetador
        CConsumidor oConsumidorD = new CConsumidor(oBufferD, oEmpaquetadorD, oEstadisticas);
        CConsumidor oConsumidorP = new CConsumidor(oBufferP, oEmpaquetadorP, oEstadisticas);
        CConsumidor oConsumidorC = new CConsumidor(oBufferC, oEmpaquetadorC, oEstadisticas);

        // Creo las maquinas, que actuan como productores de tornillo
        // Cada maquina fabrica un tipo de tornillo diferente
        CMaquina oMaquinaT1 = new CMaquina("M1", "T1", oBufferD, oBufferP, oBufferC, oAlmacen, oEstadisticas);
        CMaquina oMaquinaT2 = new CMaquina("M2", "T2", oBufferD, oBufferP, oBufferC, oAlmacen, oEstadisticas);
        CMaquina oMaquinaT3 = new CMaquina("M3", "T3", oBufferD, oBufferP, oBufferC, oAlmacen, oEstadisticas);

        // Creo el planificador de produccion, que reparte los pedidos entre las maquinas.
        CPlanificadorProduccion oPlanificador = new CPlanificadorProduccion(oMaquinaT1, oMaquinaT2, oMaquinaT3);

        // Creo el proceso coordinador, que busca los archivos de los proveedores y los procesa
        CProcesoCoordinador oCoordinador = new CProcesoCoordinador(oPlanificador, oEstadisticas);


        // Establezco las prioridades de los hilos consumidores
        oConsumidorD.setPriority(Thread.NORM_PRIORITY);
        oConsumidorP.setPriority(Thread.NORM_PRIORITY);
        oConsumidorC.setPriority(Thread.NORM_PRIORITY);

        // Establezco las prioridades de los hilos de las maquinas
        oMaquinaT1.setPriority(Thread.NORM_PRIORITY);
        oMaquinaT2.setPriority(Thread.NORM_PRIORITY);
        oMaquinaT3.setPriority(Thread.NORM_PRIORITY);
        
        // Establezco la prioridad normal para el planificador
        oPlanificador.setPriority(Thread.NORM_PRIORITY);

        // Inicio los hilos consumidores
        oConsumidorD.start();
        oConsumidorP.start();
        oConsumidorC.start();

        // Inicio los tres hilos productores (las maquinas)
        oMaquinaT1.start();
        oMaquinaT2.start();
        oMaquinaT3.start();
        
        // Inicio el hilo planificador y el hilo coordinadoor
        oPlanificador.start();

        // Inicio el coordinador de los procesos
        oCoordinador.start();

        System.out.println("Fabrica de tornillos iniciada"); 

        try {

            Scanner sc = new Scanner(System.in);

            System.out.println("Cuando hayas detenido el servidor generador con Ctrl+C, pulsa ENTER para finalizar la fabrica");

            // Espero a que el usuario confirme que ha detenido el generador
            sc.nextLine();
        
            // Solicitar la parada del coordinador
            oCoordinador.detener();

            // Espero a que termine el coordinador
            oCoordinador.join();

            // Espero a que termine el planificador
            oPlanificador.join();

            // Espero a que terminen las máquinas
            oMaquinaT1.join();
            oMaquinaT2.join();
            oMaquinaT3.join();

            // Envio una Poison Pill a cada empaquetador
            oBufferD.producir(new CTornillo(true));
            oBufferP.producir(new CTornillo(true));
            oBufferC.producir(new CTornillo(true));

            // Espero a que terminen los empaquetadores
            oConsumidorD.join();
            oConsumidorP.join();
            oConsumidorC.join();

            // Muestro el informe final
            // 10000000 es la cantidad inicial de materia prima en el almacen
            // Se resta la cantidad de materia prima que queda en el almacen para obtener la cantidad de materia prima utilizada
            oEstadisticas.mostrarInforme(10000000 - oAlmacen.getiMaterialPrima());
         
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } 

    }
}