/**
 * Oscar Martin Hernandez
 * 14/10/2026
 * Modulo: PSP
 * Clase Almacen
 * Esta clase gestiona la materia prima que se encuentra en el almacen de la fabrica
 * almacena la materia prima
 */

public class CAlmacen {

    //Este atributo no es final porque la cantidad de materia prima puede variar  
    private int iMateriaPrima; //cantidad de materia prima que hay en el almacen


	public CAlmacen(int iMaterialPrima) {
		this.iMateriaPrima = iMaterialPrima;
	}

	public int getiMaterialPrima() {
		return iMateriaPrima;
	}

	public void setiMaterialPrima(int iMaterialPrima) {
		this.iMateriaPrima = iMaterialPrima;
	}

	//Metodo sincronizado para retirar materia prima sin dejar el stock en negativo 
	public synchronized boolean retirarMaterial(int iCantidad){

		if(iCantidad <= 0){
    		return false;
		}

		if(iCantidad > iMateriaPrima){
			return false;
		}

        //Resto del almacen la cantidad de materia prima retirada.
		iMateriaPrima = iMateriaPrima - iCantidad;

		return true;
	}

	@Override
	public String toString() {
		return "CAlmacen [iMaterialPrima=" + iMateriaPrima + "]";
	}

}