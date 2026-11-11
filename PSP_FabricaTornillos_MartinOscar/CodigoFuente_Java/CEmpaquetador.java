/**
 * Oscar Martin Hernandez
 * 15/10/2026
 * Modulo: PSP
 * Clase Empaquetador
 * Esta clase representa un empaquetador de la fabrica que se encarga de empaquetar los tornillos
 * configura los empaquetadores
 */

public class CEmpaquetador{

    //final para que no se puedan modificar una vez creado el empaquetador
    private final String sIdEmpaquetador;    //id del empaquetador
    private final int iCantidadTornillos;   //cantidad de tornillos que caben en cada paquete

    // Constructor que crea un empaquetador con su identificador y capacidad
    public CEmpaquetador(String sIdEmpaquetador, int iCantidadTornillos) {
        this.sIdEmpaquetador = sIdEmpaquetador;
        this.iCantidadTornillos = iCantidadTornillos;
    }

    // Devuelve el identificador del empaquetador
    public String getsIdEmpaquetador() {
        return sIdEmpaquetador;
    }

    // Devuelve la capacidad maxima de tornillos de cada paquete
    public int getiCantidadTornillos() {
        return iCantidadTornillos;
    }

    //Sin setters porque son final y no se puede modificar una vez creado el objeto

    @Override
    public String toString() {
        return "CEmpaquetador [sIdEmpaquetador=" + sIdEmpaquetador + ", iCantidadTornillos=" + iCantidadTornillos + "]";
    }
    
}
