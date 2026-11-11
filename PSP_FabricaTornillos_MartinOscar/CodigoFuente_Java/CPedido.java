/**
 * Oscar Martin Hernandez
 * 15/10/2026
 * Modulo: PSP
 * Clase Pedido
 * Esta clase representa un pedido de tornillos que se realiza a la fabrica
 * representa los pedidos
 */

public class CPedido{

    //final para que no se puedan modificar una vez creado el pedido
    private final String sIdPedido;  //id del pedido
    private final String sTipo;      //tipo de tornillo (t1,t2 o t3)
    private final int iCantidad;     //cantidad de tornillos que se piden en el pedido
    private final String sFormatoEmpaquetado; //Formato de empaqueta del pedido (12(D),2(P) o 100(C) tornillos)

    public CPedido(String sIdPedido, String sTipo, int iCantidad, String sFormatoEmpaquetado) {
        this.sIdPedido = sIdPedido;
        this.sTipo = sTipo;
        this.iCantidad = iCantidad;
        this.sFormatoEmpaquetado = sFormatoEmpaquetado;
    }

    public String getsIdPedido() {
        return sIdPedido;
    }

    public String getsTipo() {
        return sTipo;
    }

    public int getiCantidad() {
        return iCantidad;
    }

    public String getsFormatoEmpaquetado() {
        return sFormatoEmpaquetado;
    }

    //Sin setters porque son final y no se puede modificar una vez creado el objeto

    @Override
    public String toString() {
        return "CPedido [sIdPedido=" + sIdPedido + ", sTipo=" + sTipo + ", iCantidad=" + iCantidad
                + ", sFormatoEmpaquetado=" + sFormatoEmpaquetado + "]";
    }
    
}