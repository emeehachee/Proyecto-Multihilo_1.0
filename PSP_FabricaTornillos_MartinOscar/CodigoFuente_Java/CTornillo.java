/**
 * Oscar Martin Hernandez
 * 14/10/2026
 * Modulo: PSP
 * Clase Tornillo
 * Esta clase representa un objeto tornillo que esta fabricado en la fabrica
 * representa los tornillos y las señales de finalización
 */

public class CTornillo{

    //ambos final para que no se pueda modificar una vez creado
    private final String sIdPedido; //identificadfor del pedido al que pertenece el tornillo
    private final String sTipoTornillo; //tipo de tornillo (t1,t2 o t3)
    private final boolean bFinPedido;   // Indica si el objeto representa una señal de finalizacion de pedido
    private final boolean bPoisonPill;  // Indica si el objeto representa una Poison Pill para detener al consumidor

    //Constructor para un tornillo normal
    public CTornillo(String sIdPedido, String sTipoTornillo){
        this.sIdPedido= sIdPedido;
        this.sTipoTornillo= sTipoTornillo;
        this.bFinPedido = false;
        this.bPoisonPill = false;
    }

    // Constructor para crear una señal de finalizacion de pedido
    public CTornillo(String sIdPedido, String sTipoTornillo, boolean bFinPedido) {
        this.sIdPedido= sIdPedido;
        this.sTipoTornillo= sTipoTornillo;
        this.bFinPedido= bFinPedido;
        this.bPoisonPill = false;
    }

    //Constructor para crear una Poison Pill
    // bPoisonPill: Indica si este dato es la orden para que el sistema deje de trabajar y se apague
    public CTornillo(boolean bPoisonPill) {
        this.sIdPedido = "";
        this.sTipoTornillo = "";
        this.bFinPedido = false;
        this.bPoisonPill = bPoisonPill;
    }

    public String getIdPedido(){
        return this.sIdPedido;
    }

    public String getTipo(){
        return this.sTipoTornillo;
    }

    public boolean isFinPedido() {
        return bFinPedido;
    }

    public boolean isPoisonPill() {
        return bPoisonPill;
    }
    
    //No tiene set de IdPediod ni de Tipo porque son final y no se puede modificar una vez creado el objeto

	@Override
	public String toString() {
		return "CTornillo [sIdPedido=" + sIdPedido + ", sTipo=" + sTipoTornillo + "]";
	}

}

