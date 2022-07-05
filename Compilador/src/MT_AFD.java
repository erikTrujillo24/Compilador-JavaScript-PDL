import es.upm.aedlib.Pair;

/**
 * 
 * @author 	Erik Trujillo, Irene Taratiel
 * @version	1.2
 * @since	2020-10-25
 * @lastMod	2021-01-18
 * 
 */

/* Clase que implementa la Matriz de
 * Transición del Autómata Finito 
 * Determinista del Analizador Léxico
 * del Procesador.
 */

public class MT_AFD {
		
    private Pair<Integer, String>[][] mt;

    @SuppressWarnings("unchecked")
	public MT_AFD() {
        mt = new Pair[26][255];
    }

	
	public Pair<Integer, String> getValor(int fila, char colum) {
		
		return mt[fila][colum];
	}
	
	public void addValor(int fila, int colum, Pair<Integer, String> val) {

		 mt[fila][colum] = val;
	}

	
	public void print() {
		for(int i = 0; i < mt.length; i++) {
			System.out.print("Fila " + i + " >> ");
			for(int j = 0; j < mt[i].length; j++) {
				if(mt[i][j] != null) 
					System.out.print("Columna: " + j + " >> " + mt[i][j].toString() + " | ");
			}
			System.out.print("\n");
		}
	}

}
