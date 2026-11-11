/**
 * Oscar Martin Hernandez
 * 01/10/2026
 * Modulo: PSP
 * Clase Estadisitcas
 * Esta clase gestiona las estadisticas de la fabrica de tornillos
 */

public class CEstadisticas {
    private int iArchivosProcesados; // Contador del numero de archivos procesados correctamente
    private int iArchivosRechazados; // Contador del numero de archivos rechazados por contener errores
    private int iPedidosTerminados; // Contador del numero de pedidos que se han terminado
    private int iTornillosFabricados; // Contador del numero total de tornillos fabricados
    private int iPaquetesCerrados; // Contador del numero total de paquetes cerrados
    private int iErrores;   // Contador del numero de errores producidos durante la ejecucion

    // TODOS LOS METODOS SON "synchronized" PARA EVITAR PROBLEMAS DE CONCURRENCIA

    // Metodo que incrementa el contador de archivos procesados correctamente
    public synchronized void sumarArchivoProcesado() {
        iArchivosProcesados++;
    }

    // Metodo que incrementa el contador de archivos rechazados
    public synchronized void sumarArchivoRechazado() {
        iArchivosRechazados++;
    }

    // Metodo que incrementa el contador de pedidos terminados
    public synchronized void sumarPedidoTerminado() {
        iPedidosTerminados++;
    }

    // Metodo que incrementa el contador de tornillos fabricados
    public synchronized void sumarTornilloFabricado() {
        iTornillosFabricados++;
    }

    // Metodo que incrementa el contador de paquetes cerrados
    public synchronized void sumarPaqueteCerrado() {
        iPaquetesCerrados++;
    }

    // Metodo que incrementa el contador de errores producidos durante la ejecución
    public synchronized void sumarError() {
        iErrores++;
    }

    //Muestro las estadisiticas de la fabrica, pasandole la materia prima que se ha consumido en total durante la fabricacion de los tornillos
    public void mostrarInforme(int iMateriaPrimaConsumida) {
        System.out.println("\n========== INFORME FINAL DE FABRICA ==========");
        System.out.println("Archivos procesados: " + iArchivosProcesados);
        System.out.println("Archivos rechazados: " + iArchivosRechazados);
        System.out.println("Pedidos terminados: " + iPedidosTerminados);
        System.out.println("Tornillos fabricados: " + iTornillosFabricados);
        System.out.println("Materia prima consumida: " + iMateriaPrimaConsumida);
        System.out.println("Paquetes cerrados: " + iPaquetesCerrados);
        System.out.println("Errores producidos: " + iErrores);
        System.out.println("==============================================");
    }
}

