import java.util.Iterator;

import es.upm.aedlib.map.HashTableMap;
import es.upm.aedlib.map.Map;

/**
 * 
 * @author 	Erik Trujillo, Irene Taratiel
 * @version	1.0
 * @since	2020-12-03
 * @lastMod	2020-12-03
 * 
 */

/* Clase que implementa la tabla
 * Acción-GoTo del Analizador Sintáctico
 * Ascendente del Procesador.
 */
public class TAcc {

	private static Map<Integer, Map<String, String>> TAcc = 
			new HashTableMap<Integer, Map<String, String>>();
	

	public void addValor (int estado, String[] val) {
		
		Map<String, String> map = new HashTableMap<String, String>();
		for(int i = 0; i < val.length; i=i+2) {
			map.put(val[i], val[i+1]);
		}
		
		TAcc.put(estado, map);
	}
	
	
	public String getAccion (String estado, String terminal) {
				
		return TAcc.get(Integer.parseInt(estado)).get(terminal);
	}
	
	public void print() {
		
		Iterator<Integer> it1 = TAcc.keys().iterator();
		Iterator<String> it2;
		String colum;
		int estado;
		while (it1.hasNext()) {
			estado = it1.next();
			Map<String, String> map = TAcc.get(estado);
			it2 = map.keys().iterator();
			System.out.print("Estado " + estado + ">>\t");
			while (it2.hasNext()) {
				colum = it2.next();
				System.out.print(colum + ", " + map.get(colum) + " | ");
			}
			System.out.print("\n");
		}
	}
	
}
