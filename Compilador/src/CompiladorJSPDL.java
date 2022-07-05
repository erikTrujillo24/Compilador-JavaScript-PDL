import es.upm.aedlib.Pair;
import es.upm.aedlib.map.HashTableMap;
import es.upm.aedlib.map.Map;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/**
 * 
 * @author 	Erik Trujillo Guzmán
 * @version	2.3.2
 * @since	2020-10-25
 * @lastMod	2022-06-06
 * 
 */

/* 
 * Clase que implementa el Compilador de JavaScript-PDL.
 */
public class CompiladorJSPDL {
	
	// Nombre de archivo
	private static String nameFile = "CompiladorJSPDL.java";
	
	// Autor
	private static String author = "Erik Trujillo Guzmán (Grupo 1)";
	
	// Version
	private static String version = "2.4";
	
	// Fecha de creación
	private static String since = "2020-10-25";
	
	// Fecha de última modificación
	private static String lastMod = "2022-06-06";
	
	
	
	// Carácter leído
	private static char car;
	
	// Número de línea actual
	private static int lineaActual = 1;
	
	// Número de líneas tras un salto
	// de línea
	private static int nLineas = 0;
		
	// Tabla Accion
	private static TAcc TAcc = new TAcc();

	// Variable que determina si estamos en 
	// zona de declaración
	private static boolean zonaDecl = true;
	
	// Si TSactiva = true, la tabla activa es
	// la global, si false, TSL activa
	private static boolean TSactiva = true;
	
	// Nombre de la función actual
	private static String funActual = null;
	
	// Nombre de la última función 
	private static String lastFun = "main";
	
	// Nombre de la función a llamar
	private static String llamandoFun = null;
		
	/* Pila de la lista atributos de cada símbolo
	*  gramatical para el AnSem/GCI:
	*  0 - símbolo gramatical
	*  1 - .tipo
	*  2 - desplazamiento/ancho
	*  3 - número de parámetros
	*  4 - lista de tipos de parámetros
	*  5 - tipo de retorno
	*  6 - etiqueta de función
	*  7 - info extra 
	*  8 - .lugar 
	*  9 - lista de parámetros 
	*  10 - valor de constante 
	*  11 - nombre de variable temporal */
	@SuppressWarnings("rawtypes")
	private static Stack<ArrayList> pilaAtrib = 
					new Stack<ArrayList>();
	
	// Desplazamiento para la TSG
	private static int despG = 0;
	
	// Desplazamiento para la TSL
	private static int despL;
	
	// Número de ámbitos del fichero fuente
	private static int nAmbitos = 1;
	
	// Contador para etiquetas
	private static int nEtiq = 1;
	
	// Etiquetas del GCI
	private static String Belse = "";
	private static String Bdespues = "";
	
	// Contador para direcciones temporales
	private static int nTemp = 1;
	
	// Contador para direcciones de retorno
	private static int nDirRet = 1;
	
	// Contador para número de parámetros
	private static int nParam = 0;
	
	// Acumulador para tamaño de parámetros
	private static int tamParams = 1;
	
	// Contador para las etiquetas de cadenas
	private static int nCadenas = 1;
	
	// Contador para las etiquetas de bucles
	private static int nBucles = 1;
	
		
	
	// Tabla de Palabras Reservadas
	private static String[] TPalRes = new String[11]; 
	// Tabla de Operadores Aritméticos
	private static String[] OpArit = new String[5];
	// Tabla de Operadores Relacionales
	private static String[] OpRel = new String[3];
	// Tabla de Operadores Lógicos
	private static String[] OpLog = new String[2];
	
	// Array que indica, para cada posicion, el numero
	// de consecuentes de la regla numero posicion
	// y el antecedente de dicha regla
	private static String[] reglas = new String[57];
		
	// Matriz de Transición para el Autómata Finito
	// Determinista, donde las filas son los estados
	// y las columnas los caracteres
	private static MT_AFD MT_AFD = new MT_AFD();

	// Tabla de Símbolos Global
	@SuppressWarnings("rawtypes")
	private static Map<String, ArrayList> TSG = 
					new HashTableMap<String, ArrayList>();
	
	// Tabla de Símbolos Local
	@SuppressWarnings("rawtypes")
	private static Map<String, ArrayList> TSL =
					new HashTableMap<String, ArrayList>();
	
	// Buffer de TS, que guarda el nombre de la función
	// (o "" si es la TSG) y el contenido de su TS
	@SuppressWarnings("rawtypes")
	private static ArrayList<Pair<String, Map<String, ArrayList>>> bTS = 
			new ArrayList<Pair<String, Map<String, ArrayList>>>();
	
	
	
	
	
	// Fichero tokens
	private static FileWriter FTokens;
	
	// Fichero Tabla de Símbolos
	private static FileWriter FTS;
	
	// Fichero Errores
	private static FileWriter FErr;
	
	// Fichero parse
	private static FileWriter FParse;

	// Fichero de Código Intermedio
	private static FileWriter FCI;
		
	// Fichero de Código Objeto
	private static FileWriter FCO;
	
	// Fichero temporal para el programa principal de Código Objeto
	private static FileWriter FtempMainCO;
	
	// Fichero temporal para los datos de Código Objeto
	private static FileWriter FtempDatosCO;
	
	// Fichero fuente
	private static BufferedReader bf = null;

	
	
	
	
	


	/*
	 * Función que implementa el Analizador
	 * Léxico del Procesador.
	 * Devuelve un token leído del fichero
	 * fuente.
	 */
	@SuppressWarnings("unchecked")
	public static Pair<String, String> AnLex () {
		
		int estado = 0; //Estado inicial
		String accion;
		
		String lexema = "";
		String valor = "";
		
		Pair<String, String> token = null;
				
		while (estado < 8) { // Mientras no sea un estado final
			
			// Final de fichero
			if((int) car == 65535 || car == 0) {
				return new Pair<String, String>("EOF","$");
			}
			
			// Nueva línea
			else if(car == '\n') {
				nLineas++;
				lineaActual++;
			}
			
			Pair<Integer, String> p = MT_AFD.getValor(estado, car);
			
			// Error: se ha leído un carácter no esperado
			if(p == null) {
				if(estado == 3)
					GestorErrores("AnLex", 44, "", "");
					
				else if(estado == 5)
					GestorErrores("AnLex", 45, "", "");

				else 
					GestorErrores("AnLex", 1, String.valueOf(car), "");
				
				try {
					car = (char) bf.read();
				} catch (IOException e) { e.printStackTrace(); }
				return null;
			}
			
			estado = p.getLeft();
			accion = p.getRight();
				
			if(accion == "") {	// No hay transicion
				try {
					car = (char) bf.read();
				} catch (IOException e) { e.printStackTrace(); } 
			}
			
			else { // Existe transicion
				
				char AS = accion.charAt(0);
				
				// Acciones Semánticas
				switch(AS) {
				case 'A':
					lexema = lexema + car;
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;
					
				case 'B':
					valor = valor + car;
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;
					
				case 'C':
					int pos = buscarTPalRes(lexema);
					if(pos != -1)  // lexema es una PalRes
						token = GenToken("PalRes", Integer.toString(pos)); 
					
					else { // lexema es un id
						boolean found;
						if(zonaDecl) { // En zona de declaración
							if(TSactiva) //TSG activa
								found = TSG.containsKey(lexema);
							else //TSL activa
								found = TSL.containsKey(lexema);
							
							if(found) // Variable ya declarada
								GestorErrores("AnLex", 4, lexema, "");
							else {
								@SuppressWarnings("rawtypes")
								ArrayList atribs = new ArrayList();
								atribs.add(0, lexema);
								atribs.add(1, "");
								atribs.add(2, ""); 
								atribs.add(3, "");
								ArrayList<String> tipoParam = new ArrayList<String>();
								atribs.add(4, tipoParam);
								atribs.add(5, "");
								atribs.add(6, "");
								if(TSactiva) //TSG activa
									TSG.put(lexema, atribs);
								else //TSL activa
									TSL.put(lexema, atribs);
							}
						}
						token = GenToken("id", lexema); 
					}
					break;
					
				case 'D':
					if(Integer.valueOf(valor) > 32767) {
						GestorErrores("AnLex", 2, "", "");
					}
					else 
						token = GenToken("cteEntera", valor);
					break;
					
				case 'E':
					if(lexema.length() > 64)  {
						GestorErrores("AnLex", 3, "", "");
					}
					else
						token = GenToken("cadena", lexema);
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;
					
				case 'F':
					token = GenToken("preInc", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;
					
				case 'G':
					token = GenToken("OpArit", "1");
					break;
					
				case 'H':
					token = GenToken("igual", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'I':
					token = GenToken("coma", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'J':
					token = GenToken("ptoComa", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'K':
					token = GenToken("parentAbre", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'L':
					token = GenToken("parentCierra", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'M':
					token = GenToken("llaveAbre", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'N':
					token = GenToken("llaveCierra", "");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'O':
					token = GenToken("OpRel", "1");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;

				case 'P':
					token = GenToken("OpLog", "1");
					try {
						car = (char) bf.read();
					} catch (IOException e) { e.printStackTrace(); } 
					break;
				}
			}
		}
		
		return token;
	}
		
	
	/*
	 * Método que implementa el Analizador
	 * Sintáctico Ascendete del Procesador.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void AnSt () {
		
		Pair<String, String> token = AnLex();
		String tok = tokenTransformer(token);
		String action;
		char acc; 				// accion
		String estado = "0"; 	// estado inicial
		String regla;
		String ant; 			// antecedente
		String cons; 			// consecuente
		int k; 					// nº de simbolos en consecuente
		boolean found;
		
		// Pila inicial - estado 0
		Stack<String> pila = new Stack<String>();
		pila.push(estado);
		pilaAtrib.push(new ArrayList<String>());
		
		while(true) {
			action = TAcc.getAccion(estado, tok);
			
			if(estado.equals("40"))
				AnSem("-1");
			
			if(action != null) {
				acc = action.charAt(0);
				
				// Desplazar
				if(acc == 'd') {
					nLineas = 0;
					
					estado = action.substring(1, action.length());
					pila.push(tok);
					
					ArrayList arr = new ArrayList(11);
					if(tok.equals("id")) {
						String id = token.getRight();
						if(!TSactiva) { // TSL activa
							found = TSL.containsKey(id);
							if(!found && TSG.containsKey(id))
								arr = TSG.get(id);
							else
								arr.add(token.getRight());
						}
						else if(TSG.containsKey(id)) 
							arr = TSG.get(id);
						else
							arr.add(token.getRight());
					}
					else if(tok.equals("ent") || tok.equals("cad")) {
						for(int i = 0; i < 12; i++) {
							arr.add("");
						}
						arr.set(0, tok);
						arr.set(10, token.getRight());
					}
					else
						arr.add(tok);
					pilaAtrib.push(arr);
					
					pila.push(estado);
					pilaAtrib.push(new ArrayList<String>());
					
					
					if(tok.equals("function") || tok.equals("let"))
						AnSem("0");
					else if(tok.equals(";"))
						AnSem("-1");
					
					else if(tok.equals(")") 
							&& pilaAtrib.get(pilaAtrib.size()-8).get(0).equals("if"))
						AnSem("-6_1");
					
					else if(tok.equals("}") && (pilaAtrib.size() >= 14)
							&& pilaAtrib.get(pilaAtrib.size()-14).get(0).equals("if"))
						AnSem("-6_2");
					
					// Se pide el siguiente token al AnLex
					token = AnLex();
					
					// Si el AnLex detecta error
					if(token == null) 
						break;
					
					tok = tokenTransformer(token);
				}
				
				// Reducir
				else if(acc == 'r') {
					regla = action.substring(1, action.length());
					cons = reglas[Integer.parseInt(regla)];
					k = Integer.parseInt(cons.substring(0, 1));
					ant = cons.substring(1, cons.length());
					
					// Se escribe el número de regla
					// en el fichero de parse
					try {
						FParse.write(regla + " ");
					} catch (IOException e) { e.printStackTrace(); }
	
					// Se saca el consecuente de la pila
					for(int i = 0; i < 2*k; i++) {
						pila.pop();
					}
					
					estado = TAcc.getAccion(pila.lastElement(), ant);
					// Se mete el antecedente en la pila
					pila.push(ant);
					// Se mete el GOTO en la pila
					pila.push(estado);
					
					// Se llama al AnSem 
					AnSem(regla);
					
					if(!tok.equals("$"))
						nLineas = 0;
				}
				
				// Aceptar la cadena
				else if(acc == 'a') {
					nLineas = 0;
					break;
				}
			}
			
			// Error
			else {
				if(tok.equals("id")) // Si es un id se muestra su lexema en el error
					buscarError(estado, token.getRight());
				else if(!TSactiva && tok.equals("function"))
					GestorErrores("AnSt", 43, "", "");
				else // Se muestra el terminal en el error
					buscarError(estado, tok);
				break;
			}
		}
	}
	
	
	/*
	 * Método que implementa el Analizador
	 * Semántico del Procesador.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void AnSem (String nRegla) {
								
		if(nRegla.equals("0")) {
			zonaDecl = true;
			return;
		}
		
		else if(nRegla.equals("-1")) {
			zonaDecl = false;
			return;
		}
		
		int cima = pilaAtrib.size() - 1;

		if(nRegla.equals("-6_1")) {
			Bdespues = nuevaEtiq();
			Belse = nuevaEtiq();
			
			// Se guardan las etiquetas Bdespues y Belse
			ArrayList array = pilaAtrib.get(cima-3);
			array.set(7, Bdespues+","+Belse);
			pilaAtrib.set(cima-3, array);

			ArrayList arr = pilaAtrib.get(cima-3);
			if(arr.get(1).equals("log")) {
				arr.set(1, "tipo_ok");
				pilaAtrib.set(cima-3, arr);
				// GCI
				emite("goto_==", new Pair<String,String>((((String) pilaAtrib.get(cima-3).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable((String) pilaAtrib.get(cima-3).get(11)), (String) pilaAtrib.get(cima-3).get(8)),
						new Pair<String,String>("cte_ent", "0"), new Pair<String,String>("etiq", Belse));
				writeToCIFile("if " + pilaAtrib.get(cima-3).get(11) + " == 0 goto " + Belse);
			}
			else {
				arr.set(1, "tipo_error");
				pilaAtrib.set(cima-3, arr);
				GestorErrores("AnSem", 5, "", "");
			}
			return;
		}
		
		if(nRegla.equals("-6_2")) {
			// Se obtienen las etiquetas Bdespues y Belse
			String[] etiqs = ((String) pilaAtrib.get(cima-9).get(7)).split(",");
			
			emite("goto", new Pair<String,String>("etiq", etiqs[0]),
					new Pair<String,String>("",""), new Pair<String,String>("",""));
			writeToCIFile("goto "+etiqs[0]);
			emite(":", new Pair<String,String>("etiq", etiqs[1]),
					new Pair<String,String>("",""), new Pair<String,String>("",""));
			writeToCIFile(etiqs[1] + ":");
			return;
		}

		int regla = Integer.parseInt(nRegla);
		String cons = reglas[regla];
		int k = Integer.parseInt(cons.substring(0, 1));
		String ant = cons.substring(1, cons.length());
		
		// Array de atributos del antecedente
		ArrayList atribs = new ArrayList(12);
		ArrayList arr = new ArrayList(12);
		for(int i = 0; i < 12; i++) {
			atribs.add("");
			arr.add("");
		}
		// Se añade el antecedente
		atribs.add(0, ant);
		boolean found;
		String id;
		
		ArrayList<String> varTemp;
		
		// Acciones semánticas
		switch(regla) {
		case 1:
			// Borrar contenido de TSG
			Iterator<String> iter = TSG.keys().iterator();
			while(iter.hasNext())
				TSG.remove(iter.next());
			break;
			
		case 2:
			if(pilaAtrib.get(cima-3).get(1).equals("tipo_vacio"))
				atribs.set(1, (String) pilaAtrib.get(cima-1).get(1));
			else if(pilaAtrib.get(cima-3).get(1).equals("tipo_error") ||
					pilaAtrib.get(cima-1).get(1).equals("tipo_error"))
				atribs.set(1, "tipo_error");
			else
				atribs.set(1, "tipo_ok");
			break;
			
		case 3:
			if(pilaAtrib.get(cima-3).get(1).equals("tipo_vacio"))
				atribs.set(1, (String) pilaAtrib.get(cima-1).get(1));
			else if(pilaAtrib.get(cima-3).get(1).equals("tipo_error") ||
					pilaAtrib.get(cima-1).get(1).equals("tipo_error"))
				atribs.set(1, "tipo_error");
			else
				atribs.set(1, "tipo_ok");
			break;
			
		case 4:
			atribs.set(1, "tipo_vacio");
			break;
			
		case 5:
			id = (String) pilaAtrib.get(cima-5).get(0);
			int Tancho = Integer.valueOf((String) pilaAtrib.get(cima-7).get(2));
			arr.set(0, id);
			arr.set(1, (String) pilaAtrib.get(cima-7).get(1)); // tipo
			arr.set(4, new ArrayList<String>());
			
			if(TSactiva) { // TSG activa
				arr.set(2, String.valueOf(despG)); //despG
				TSG.put(id, arr);
				despG = despG + Tancho;
			}
			else { // TSL activa
				arr.set(2, String.valueOf(despL)); //despL
				TSL.put(id, arr);
				despL = despL + Tancho;
			}
			
			pilaAtrib.set(cima-5, arr);
		
			if(pilaAtrib.get(cima-5).get(1).equals(pilaAtrib.get(cima-3).get(1))) {
				atribs.set(1, "tipo_ok");
				// GCI
				if(pilaAtrib.get(cima-5).get(1).equals("cad")) {
					emite("=cad", new Pair<String,String>("varTemp", (String) pilaAtrib.get(cima-3).get(8)),
							new Pair<String,String>("",""), new Pair<String,String>((TSactiva)?"varGlobal":"varLocal", buscarLugarTS(id)));
					writeToCIFile(id + " :=cad " + pilaAtrib.get(cima-3).get(11));
				}
				else {
					emite("=ent", new Pair<String,String>("varTemp", (String) pilaAtrib.get(cima-3).get(8)),
							new Pair<String,String>("",""), new Pair<String,String>((TSactiva)?"varGlobal":"varLocal", buscarLugarTS(id)));
					writeToCIFile(id + " :=ent " + pilaAtrib.get(cima-3).get(11));
				}
			}
			else if (pilaAtrib.get(cima-3).get(1).equals("tipo_vacio")) {
				atribs.set(1, "tipo_ok");
			}
			else {
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 7, "", "");
			}
			break;
			
		case 6:
			if(pilaAtrib.get(cima-11).get(1).equals("tipo_ok") &&
			   pilaAtrib.get(cima-5).get(1).equals("tipo_ok")) {
				if(pilaAtrib.get(cima-1).get(1).equals("tipo_vacio") ||
					pilaAtrib.get(cima-1).get(1).equals("tipo_ok")) 
					atribs.set(1, "tipo_ok");
				
				else
					atribs.set(1, "tipo_error");
			}
				
			else 
				atribs.set(1, "tipo_error");
			
			//GCI
			// Se obtienen las etiquetas Bdespues y Belse
			String[] etiqs = ((String) pilaAtrib.get(cima-11).get(7)).split(",");

			emite(":", new Pair<String,String>("etiq", etiqs[0]),
					new Pair<String,String>("",""), new Pair<String,String>("",""));
			writeToCIFile(etiqs[0]+": ");
			break;
			
		case 7:
			atribs.set(1, pilaAtrib.get(cima-3).get(1));
			break;
			
		case 8:
			atribs.set(1, "tipo_vacio");
			break;
			
		case 9:
			if(pilaAtrib.get(cima-1).get(1).equals("tipo_error")
				|| pilaAtrib.get(cima-1).get(5).equals("tipo_error"))
				atribs.set(1, "tipo_error");
			else
				atribs.set(1, pilaAtrib.get(cima-1).get(1));
			break;
			
		case 10:
			atribs.set(1, pilaAtrib.get(cima-1).get(1));
			// GCI
			atribs.set(8, pilaAtrib.get(cima-1).get(8));
			atribs.set(11, pilaAtrib.get(cima-1).get(11));
			break;
			
		case 11:
			atribs.set(1, "tipo_vacio");
			break;
			
		case 12:
			String tipoC1 = (String) pilaAtrib.get(cima-3).get(1);
			String tipoB = (String) pilaAtrib.get(cima-1).get(1);
			
			if(tipoC1.equals("tipo_error") || tipoB.equals("tipo_error")) {
				atribs.set(1, "tipo_error");
			}
			else 
				atribs.set(1, pilaAtrib.get(cima-1).get(1));
			
			atribs.set(5, pilaAtrib.get(cima-1).get(5));
			break;
			
		case 13:
			atribs.set(1, "tipo_vacio");
			break;
			
		case 14:
			atribs.set(1, "ent");
			atribs.set(2, "1");
			break;
			
		case 15:
			atribs.set(1, "cad");
			atribs.set(2, "64");
			break;
			
		case 16:
			atribs.set(1, "log");
			atribs.set(2, "1");
			break;
			
		case 17:
			id = (String) pilaAtrib.get(cima-5).get(0);

			// Se busca el id en las TS
			if(!TSactiva) { // TSL activa
				if(TSL.containsKey(id))
					arr = TSL.get(id);
				else if (TSG.containsKey(id))
						arr = TSG.get(id);
				else {
					arr.set(0, id);
					arr.set(1, "ent");
					arr.set(2, String.valueOf(despG));
					arr.set(4, new ArrayList<String>());
					TSG.put(id, arr);
					despG = despG + 1;	
				}
			}
			else { // TSG activa
				if(TSG.containsKey(id))
					arr = TSG.get(id);
				else {
					arr.set(0, id);
					arr.set(1, "ent");
					arr.set(2, String.valueOf(despG));
					arr.set(4, new ArrayList<String>());
					TSG.put(id, arr);
					despG = despG + 1;	
				}
			}
			
			pilaAtrib.set(cima-5, arr);
							
			// Llamada a función
			if(arr.get(1).equals("fun")) {
				String fun = (String) pilaAtrib.get(cima-5).get(0); 

				if(pilaAtrib.get(cima-3).get(1).equals("fun")) {
					String numParam = (String) arr.get(3);
					ArrayList<String> tipoParam = (ArrayList<String>) arr.get(4);
					ArrayList<String> NtipoParam = (ArrayList) pilaAtrib.get(cima-3).get(4);
					
					if(pilaAtrib.get(cima-3).get(3).equals(numParam)
						&& NtipoParam.equals(tipoParam)) {
						atribs.set(1, "tipo_ok");
						// GCI
						ArrayList<Pair<String,String>> params =
								(ArrayList<Pair<String,String>>) pilaAtrib.get(cima-3).get(9);
						int QnumParam = 0;
						if (!pilaAtrib.get(cima-3).get(3).equals(""))
							QnumParam =  Integer.valueOf((String) pilaAtrib.get(cima-3).get(3));
						
						llamandoFun = fun;
						nParam = QnumParam;
												
						for(int i = QnumParam-1; i > -1; i--) {
							emite("param", new Pair<String,String>((((String) params.get(i).getLeft()).startsWith("_temp"))?"varTemp":buscarClaseVariable(params.get(i).getLeft()), params.get(i).getRight()),
									new Pair<String,String>("",""), new Pair<String,String>("",""));
							writeToCIFile("param " + params.get(i).getLeft());
						}
						emite("call", new Pair<String,String>("etiq", (String) arr.get(6)),
								new Pair<String,String>("",""), new Pair<String,String>("",""));
						writeToCIFile("call " + (String) arr.get(6));
					}
					else if (tipoParam.size() == 0 && NtipoParam.size() != 0) {
						atribs.set(1, "tipo_error");
						GestorErrores("AnSem", 13, fun, "");
					}
					
					else {
						atribs.set(1, "tipo_error");
						GestorErrores("AnSem", 6, fun, "");
					}
				}
				
				else {
					atribs.set(1, "tipo_error");
					GestorErrores("AnSem", 6, fun, "");
				}
			}
			
			else if (pilaAtrib.get(cima-3).get(1).equals("fun")) {
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 15, id, "");
			}
			
			else { // Sentencia de asignación
				if(arr.get(1).equals(pilaAtrib.get(cima-3).get(1))) {
					atribs.set(1, "tipo_ok");
					// GCI
					if(arr.get(1).equals("cad")) {
						emite("=cad", new Pair<String,String>((((String) pilaAtrib.get(cima-3).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable(id), (String) pilaAtrib.get(cima-3).get(8)),
								new Pair<String,String>("",""), new Pair<String,String>(buscarClaseVariable(id), buscarLugarTS(id)));
						writeToCIFile(id + " :=cad \"" + (String) pilaAtrib.get(cima-3).get(11) + "\"");
					}
					else {
						emite("=ent", new Pair<String,String>((((String) pilaAtrib.get(cima-3).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable(id), (String) pilaAtrib.get(cima-3).get(8)),
								new Pair<String,String>("",""), new Pair<String,String>(buscarClaseVariable(id), buscarLugarTS(id)));
						writeToCIFile(id + " :=ent " + (String) pilaAtrib.get(cima-3).get(11));
					}
				}
				else {
					atribs.set(1, "tipo_error");
					GestorErrores("AnSem", 7, "", "");
				}
			}
			break;
			
		case 18:
			atribs.set(1, "fun");
			atribs.set(3, pilaAtrib.get(cima-3).get(3));
			atribs.set(4, pilaAtrib.get(cima-3).get(4));
			// GCI
			atribs.set(9, pilaAtrib.get(cima-3).get(9));
			atribs.set(11, pilaAtrib.get(cima-3).get(11));
			break;
			
		case 19:
			atribs.set(1, pilaAtrib.get(cima-1).get(1));
			// GCI
			atribs.set(8, pilaAtrib.get(cima-1).get(8));
			atribs.set(11, pilaAtrib.get(cima-1).get(11));
			break;
			
		case 20:
			if(!TSactiva) { // TSL activa
				String tipoRet = (String) TSG.get(funActual).get(5);
				
				if(pilaAtrib.get(cima-3).get(1).equals("tipo_error")) {
					atribs.set(1, "tipo_error");
					atribs.set(5, "tipo_error");
				}
				
				else if (tipoRet.equals("tipo_vacio")) {
					if(pilaAtrib.get(cima-3).get(1).equals("tipo_vacio")) {
						atribs.set(1, "tipo_ok");
						atribs.set(5, tipoRet);
						// GCI
						emite("return", new Pair<String,String>("",""),
								new Pair<String,String>("",""), new Pair<String,String>("",""));
						writeToCIFile("return");
					}
					else {
						atribs.set(1, "tipo_error");
						atribs.set(5, "tipo_error");
						GestorErrores("AnSem", 16, funActual, "");
					}
				}
				
				else if(pilaAtrib.get(cima-3).get(1).equals(tipoRet)) {
					atribs.set(1, "tipo_ok");
					atribs.set(5, tipoRet);
					// GCI
					emite("return", new Pair<String,String>("",""),
							new Pair<String,String>("",""), new Pair<String,String>((((String) pilaAtrib.get(cima-3).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable((String) pilaAtrib.get(cima-3).get(11)), (String) pilaAtrib.get(cima-3).get(8)));
					writeToCIFile("return " + (String) pilaAtrib.get(cima-3).get(11));
				}
				else {
					if(tipoRet.equals("ent"))
						tipoRet = "number";
					else if(tipoRet.equals("log"))
						tipoRet = "boolean";
					else if(tipoRet.equals("cad"))
						tipoRet = "string";
					GestorErrores("AnSem", 17, funActual, tipoRet);
				}
			}
			else {
				atribs.set(1, "tipo_ok");
				atribs.set(5, pilaAtrib.get(cima-3).get(1));
				// GCi
				emite("return", new Pair<String,String>("",""),
						new Pair<String,String>("",""), new Pair<String,String>("",""));
				writeToCIFile("return");
			}
			break;
			
		case 21:
			if(pilaAtrib.get(cima-5).get(1).equals("ent") ||
				pilaAtrib.get(cima-5).get(1).equals("cad")) {
				atribs.set(1, "tipo_ok");
				// GCI
				id = (String) pilaAtrib.get(cima-5).get(11);
				if(pilaAtrib.get(cima-5).get(1).equals("ent")) {
					emite("alert_ent", new Pair<String,String>("",""),
							new Pair<String,String>("",""), new Pair<String,String>((((String) pilaAtrib.get(cima-5).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable(id), (String) pilaAtrib.get(cima-5).get(8)));
					writeToCIFile("alert_ent " + (String) pilaAtrib.get(cima-5).get(8));
				}
				else {
					emite("alert_cad", new Pair<String,String>("",""),
							new Pair<String,String>("",""), new Pair<String,String>((((String) pilaAtrib.get(cima-5).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable(id), (String) pilaAtrib.get(cima-5).get(8)));
					writeToCIFile("alert_cad " + (String) pilaAtrib.get(cima-5).get(11));
				}
			}
			else {
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 8, "", "");
			}
			break;
			
		case 22:
			id = (String) pilaAtrib.get(cima-5).get(0);
			if(!TSactiva) { // TSL activa
				if(TSL.containsKey(id))
					arr = TSL.get(id);
				else if (TSG.containsKey(id))
					arr = TSG.get(id);
				else {
					arr.set(0, id);
					arr.set(1, "ent");
					arr.set(2, String.valueOf(despG));
					arr.set(4, new ArrayList<String>());
					TSG.put(id, arr);
					despG = despG + 1;
				}	
			}
			else { // TSG activa
				if(TSG.containsKey(id))
					arr = TSG.get(id);
				else {
					arr.set(0, id);
					arr.set(1, "ent");
					arr.set(2, String.valueOf(despG));
					arr.set(4, new ArrayList<String>());
					TSG.put(id, arr);
					despG = despG + 1;
				}
			}
			
			pilaAtrib.set(cima-5, arr);
							
			if(arr.get(1).equals("ent") || arr.get(1).equals("cad")) {
				atribs.set(1, "tipo_ok");
				// GCI
				if(arr.get(1).equals("ent")) {
					emite("input_ent", new Pair<String,String>("",""),
							new Pair<String,String>("",""), new Pair<String,String>(buscarClaseVariable(id), buscarLugarTS(id)));
					writeToCIFile("input_ent " + id);
				}
				else {
					emite("input_cad", new Pair<String,String>("",""),
							new Pair<String,String>("",""), new Pair<String,String>(buscarClaseVariable(id), buscarLugarTS(id)));
					writeToCIFile("input_cad " + id);
				}
			}
			else {
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 9, "", "");
			}
			break;
				
		case 23:
			atribs.set(1, pilaAtrib.get(cima-1).get(1));
			// GCI
			atribs.set(8, pilaAtrib.get(cima-1).get(8));
			atribs.set(11, pilaAtrib.get(cima-1).get(11));
			break;
			
		case 24:
			atribs.set(1, "tipo_vacio");
			break;
			
		case 25:
			if(pilaAtrib.get(cima-1).get(1).equals("ent") &&
				pilaAtrib.get(cima-5).get(1).equals("ent")) {
				atribs.set(1, "log");
				// GCI
				varTemp = nuevaTemp(atribs);
				atribs.set(8, varTemp.get(1));
				atribs.set(11, varTemp.get(0));
				String etiq1 = nuevaEtiq();
				String etiq2 = nuevaEtiq();
				emite("goto_>", new Pair<String,String>((((String) pilaAtrib.get(cima-5).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable((String) pilaAtrib.get(cima-5).get(11)), (String) pilaAtrib.get(cima-5).get(8)),
						new Pair<String,String>((((String) pilaAtrib.get(cima-1).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable((String) pilaAtrib.get(cima-1).get(11)), (String) pilaAtrib.get(cima-1).get(8)), new Pair<String,String>("etiq", etiq1));
				writeToCIFile("if " + (String) pilaAtrib.get(cima-5).get(11) + " > "+ (String) pilaAtrib.get(cima-1).get(11) + " goto "+etiq1);
				emite("=ent", new Pair<String,String>("cte_ent", "0"),
						new Pair<String,String>("",""), new Pair<String,String>("varTemp", (String) atribs.get(8)));
				writeToCIFile(atribs.get(11) + " :=ent 0");
				emite("goto", new Pair<String,String>("etiq", etiq2),
						new Pair<String,String>("",""), new Pair<String,String>("",""));
				writeToCIFile("goto "+etiq2);
				emite(":", new Pair<String, String>("etiq", etiq1), new Pair<String, String>("", ""), new Pair<String, String>("", ""));
				writeToCIFile(etiq1+":");
				emite("=ent", new Pair<String,String>("cte_ent", "1"),
						new Pair<String,String>("",""), new Pair<String,String>("varTemp", (String) atribs.get(8)));
				writeToCIFile(atribs.get(11) + " :=ent 1");
				emite(":", new Pair<String, String>("etiq", etiq2), new Pair<String, String>("", ""), new Pair<String, String>("", ""));
				writeToCIFile(etiq2+":");
			}
			else {
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 10, "", "");
			}
			break;
				
		case 26:
			atribs.set(1, pilaAtrib.get(cima-1).get(1));
			// GCI
			atribs.set(8,  pilaAtrib.get(cima-1).get(8));
			atribs.set(11, pilaAtrib.get(cima-1).get(11));
			break;
			
		case 27:
			if(pilaAtrib.get(cima-1).get(1).equals("ent") &&
					pilaAtrib.get(cima-5).get(1).equals("ent")) {
				atribs.set(1, "ent");
				// GCI
				varTemp = nuevaTemp(atribs);
				atribs.set(8, varTemp.get(1));
				atribs.set(11, varTemp.get(0));
				emite("+", new Pair<String,String>((((String) pilaAtrib.get(cima-5).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable((String) pilaAtrib.get(cima-5).get(11)), (String) pilaAtrib.get(cima-5).get(8)),
						new Pair<String,String>((((String) pilaAtrib.get(cima-1).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable((String) pilaAtrib.get(cima-1).get(11)), (String) pilaAtrib.get(cima-1).get(8)), new Pair<String,String>("varTemp", (String) atribs.get(8)));
				writeToCIFile(atribs.get(11) + " :=ent " + pilaAtrib.get(cima-5).get(11) + " + " + pilaAtrib.get(cima-1).get(11));
			}
			else {
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 11, "", "");
			}
			break;
					
		case 28:
			atribs.set(1, pilaAtrib.get(cima-1).get(1));
			// GCI
			atribs.set(8, pilaAtrib.get(cima-1).get(8));
			atribs.set(11, pilaAtrib.get(cima-1).get(11));
			break;
			
		case 29:
			if(pilaAtrib.get(cima-1).get(1).equals("log")) {
				atribs.set(1, "log");
				// GCI
				varTemp = nuevaTemp(atribs);
				atribs.set(8, varTemp.get(1));
				atribs.set(11, varTemp.get(0));
				emite("!", new Pair<String,String>((((String) pilaAtrib.get(cima-1).get(11)).startsWith("_temp"))?"varTemp":buscarClaseVariable((String) pilaAtrib.get(cima-1).get(11)), (String) pilaAtrib.get(cima-1).get(8)),
						new Pair<String,String>("",""), new Pair<String,String>("varTemp", (String) atribs.get(8)));
				writeToCIFile(atribs.get(11) + " :=ent !" + pilaAtrib.get(cima-1).get(11));
			}
			else {
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 12, "", "");
			}
			break;
			
		case 30:
			atribs.set(1, pilaAtrib.get(cima-1).get(1));
			// GCI
			atribs.set(8, pilaAtrib.get(cima-1).get(8));
			atribs.set(11, pilaAtrib.get(cima-1).get(11));
			break;
			
		case 31:
			atribs.set(1, pilaAtrib.get(cima-3).get(1));
			// GCI
			atribs.set(8, pilaAtrib.get(cima-3).get(8));
			atribs.set(11, pilaAtrib.get(cima-3).get(11));
			break;
			
		case 32:
			id = (String) pilaAtrib.get(cima-3).get(0);
			
			// Se busca el id en las TS
			if(!TSactiva) { // TSL activa
				found = TSL.containsKey(id);
				if(found)
					arr = TSL.get(id);
				else {
					found = TSG.containsKey(id);
					if(found)
						arr = TSG.get(id);
					else {
						arr.set(0, id);
						arr.set(1, "ent");
						arr.set(2, String.valueOf(despG));
						arr.set(4, new ArrayList<String>());
						TSG.put(id, arr);
						despG = despG + 1;	
					}
				}
			}
			else {
				found = TSG.containsKey(id);
				if(found)
					arr = TSG.get(id);
				else {
					arr.set(0, id);
					arr.set(1, "ent");
					arr.set(2, String.valueOf(despG));
					arr.set(4, new ArrayList<String>());
					TSG.put(id, arr);
					despG = despG + 1;
				}
			}
			
			pilaAtrib.set(cima-3, arr);
							
			if(arr.get(1).equals("fun")) { // Llamada a función
				String numParam = (String) arr.get(3);
				ArrayList<String> tipoParam = (ArrayList<String>) arr.get(4);
				ArrayList<String> QtipoParam = (ArrayList<String>) pilaAtrib.get(cima-1).get(4);

				if(pilaAtrib.get(cima-1).get(1).equals("tipo_vacio")) {
					atribs.set(1, "tipo_error");
					GestorErrores("AnSem", 6, (String) pilaAtrib.get(cima-3).get(0), "");
				}
								
				else if(pilaAtrib.get(cima-1).get(3).equals(numParam)
					&& QtipoParam.equals(tipoParam)) {
					
					// Sin pre-incremento
					if(pilaAtrib.get(cima-5).get(1).equals("tipo_vacio")) {
						atribs.set(1, pilaAtrib.get(cima-3).get(5));
						//GCI
						varTemp = nuevaTemp(atribs);
						atribs.set(8, varTemp.get(1));
						atribs.set(11, varTemp.get(0));
						ArrayList<Pair<String,String>> params =
								(ArrayList<Pair<String,String>>) pilaAtrib.get(cima-1).get(9);
						int QnumParam = 0;
						if (!pilaAtrib.get(cima-1).get(3).equals(""))
							QnumParam =  Integer.valueOf((String) pilaAtrib.get(cima-1).get(3));
						
						llamandoFun = (String) pilaAtrib.get(cima-3).get(0);
						nParam = QnumParam;
						
						for(int i = QnumParam-1; i > -1; i--) {
							emite("param", new Pair<String,String>((((String) params.get(i).getLeft()).startsWith("_temp"))?"varTemp":buscarClaseVariable(params.get(i).getLeft()), params.get(i).getRight()),
									new Pair<String,String>("",""), new Pair<String,String>("",""));
							writeToCIFile("param " + params.get(i).getLeft());
						}
						emite("call_ret", new Pair<String,String>("etiq", (String) arr.get(6)),
								new Pair<String,String>("",""), new Pair<String,String>("varTemp", (String) atribs.get(8)));
						writeToCIFile(atribs.get(11) + " := call " + arr.get(6));
					}
					
					else {
						atribs.set(1, "tipo_error");
						GestorErrores("AnSem", 46, "", "");
					}
				}
				
				else if (tipoParam.size() == 0 &&
						QtipoParam.size() != 0) {
					atribs.set(1, "tipo_error");
					GestorErrores("AnSem", 13, (String) pilaAtrib.get(cima-3).get(0), "");
				}
				
				else {
					atribs.set(1, "tipo_error");
					GestorErrores("AnSem", 6, (String) pilaAtrib.get(cima-3).get(0), "");
				}
			}
			
			else if(!pilaAtrib.get(cima-1).get(1).equals("tipo_vacio")) { 
				atribs.set(1, "tipo_error");
				GestorErrores("AnSem", 15, id, "");
			}
			
			else if(!pilaAtrib.get(cima-5).get(1).equals("tipo_vacio")) {
				if(arr.get(1).equals("ent")) {
					atribs.set(1, "ent");
					// GCI
					varTemp = nuevaTemp(atribs);
					atribs.set(8, varTemp.get(1));
					atribs.set(11, varTemp.get(0));
					String idLugar = buscarLugarTS(id);
					emite("+", new Pair<String,String>((TSactiva)?"varGlobal":"varLocal", idLugar),
							 new Pair<String,String>("cte_ent", "1"), new Pair<String,String>((TSactiva)?"varGlobal":"varLocal", idLugar));
					writeToCIFile(id + " :=ent " + id + " + 1");
					emite("=ent", new Pair<String,String>((TSactiva)?"varGlobal":"varLocal", idLugar),
							new Pair<String,String>("",""), new Pair<String,String>("varTemp", (String) atribs.get(8)));
					writeToCIFile(atribs.get(11) + " :=ent " + id);
				}
				else {
					atribs.set(1, "tipo_error");
					GestorErrores("AnSem", 14, "", "");
				}
			}
			
			else {
				atribs.set(1, arr.get(1));
				// GCI
				atribs.set(8, buscarLugarTS(id));
				atribs.set(11, id);
			}
			break;
			
		case 33:
			atribs.set(1, "tipo_vacio");
			atribs.set(4, new ArrayList<String>());
			break;
		
		case 34:
			atribs.set(3, pilaAtrib.get(cima-3).get(3));
			atribs.set(4, pilaAtrib.get(cima-3).get(4));
			// GCI
			atribs.set(9, pilaAtrib.get(cima-3).get(9));
			atribs.set(11, pilaAtrib.get(cima-3).get(11));
			break;
			
		case 35:
			atribs.set(1, "ent");
			// GCI
			varTemp = nuevaTemp(atribs);
			atribs.set(8, varTemp.get(1));
			atribs.set(11, varTemp.get(0));
			emite("=ent", new Pair<String,String>("cte_ent", (String) pilaAtrib.get(cima-1).get(10)),
					new Pair<String,String>("",""), new Pair<String,String>("varTemp", (String) atribs.get(8)));
			writeToCIFile(atribs.get(11) + " :=ent " + pilaAtrib.get(cima-1).get(10));
			break;
			
		case 36:
			atribs.set(1, "cad");
			// GCI
			varTemp = nuevaTemp(atribs);
			atribs.set(8, varTemp.get(1));
			atribs.set(11, varTemp.get(0));
			emite("=cad", new Pair<String,String>("cte_cad", (String) pilaAtrib.get(cima-1).get(10)),
					new Pair<String,String>("",""), new Pair<String,String>("varTemp", (String) atribs.get(8)));
			writeToCIFile(atribs.get(11) + " :=cad \"" + pilaAtrib.get(cima-1).get(10) + "\"");
			break;
			
		case 37:
			arr = (ArrayList) pilaAtrib.get(cima-1).get(4);
			if(arr.size() == 0) {
				atribs.set(3, "1");
				ArrayList<String> arr1 = new ArrayList<String>();
				arr1.add((String) pilaAtrib.get(cima-3).get(1));
				atribs.set(4, arr1);
				// GCI
				ArrayList<Pair<String,String>> arrParams = new ArrayList<Pair<String,String>>();
				arrParams.add(new Pair<String,String>((String) pilaAtrib.get(cima-3).get(11), (String) pilaAtrib.get(cima-3).get(8)));
				atribs.set(9, arrParams);
				atribs.set(11, pilaAtrib.get(cima-3).get(11));
			}
			else {
				int numParam = Integer.valueOf((String) pilaAtrib.get(cima-1).get(3)) + 1;
				atribs.set(3, String.valueOf(numParam));
				ArrayList<String> arrTipoParams = (ArrayList<String>) pilaAtrib.get(cima-1).get(4);
				arrTipoParams.add(0, (String) pilaAtrib.get(cima-3).get(1));
				atribs.set(4, arrTipoParams);
				// GCI
				ArrayList<Pair<String,String>> arrParams =
						(ArrayList<Pair<String,String>>) pilaAtrib.get(cima-1).get(9);
				arrParams.add(0, new Pair<String, String>((String) pilaAtrib.get(cima-3).get(11), (String) pilaAtrib.get(cima-3).get(8)));
				atribs.set(9, arrParams);
				atribs.set(11, pilaAtrib.get(cima-3).get(11));
			}
			break;
			
		case 38:
			atribs.set(4, new ArrayList<String>());
			// GCI
			atribs.set(9, new ArrayList<Pair<String,String>>());
			break;
			
		case 39:
			arr = (ArrayList) pilaAtrib.get(cima-1).get(4);
			if(arr.size() == 0) {
				atribs.set(3, "1");
				ArrayList<String> arr1 = new ArrayList<String>();
				arr1.add((String) pilaAtrib.get(cima-3).get(1));
				atribs.set(4, arr1);
				// GCI
				ArrayList<Pair<String,String>> arrParams = new ArrayList<Pair<String,String>>();
				arrParams.add(new Pair<String,String>((String) pilaAtrib.get(cima-3).get(11), (String) pilaAtrib.get(cima-3).get(8)));
				atribs.set(9, arrParams);
				atribs.set(11, pilaAtrib.get(cima-3).get(11));
			}
			else {
				int numParam = Integer.valueOf((String) pilaAtrib.get(cima-1).get(3)) + 1;
				atribs.set(3, String.valueOf(numParam));
				ArrayList<String> arrTipoParams = (ArrayList<String>) pilaAtrib.get(cima-1).get(4);
				arrTipoParams.add(0, (String) pilaAtrib.get(cima-3).get(1));
				atribs.set(4, arrTipoParams);
				// GCI
				ArrayList<Pair<String,String>> arrParams =
						(ArrayList<Pair<String,String>>) pilaAtrib.get(cima-1).get(9);
				arrParams.add(0, new Pair<String,String>((String) pilaAtrib.get(cima-3).get(11), (String) pilaAtrib.get(cima-3).get(8)));
				atribs.set(9, arrParams);
				atribs.set(11, pilaAtrib.get(cima-3).get(11));
			}
			break;
			
		case 40:
			atribs.set(4, new ArrayList<String>());
			// GCI
			atribs.set(9, new ArrayList<String>());
			break;
			
		case 41:
			atribs.set(1, "tipo_ok");
			break;
			
		case 42:
			atribs.set(1, "tipo_vacio");
			break;
			
		case 43:
			atribs.set(1, (String) pilaAtrib.get(cima-1).get(1));
			
			// Se escribe el contenido de la TSL en 
			// el buffer de TSL
			bTS.add(new Pair<String, Map<String, ArrayList>>((String) pilaAtrib.get(cima-5).get(7), 
							new HashTableMap<String, ArrayList>(TSL)));

			// Borrar contenido de TSL
			Iterator<String> it = TSL.keys().iterator();
			while(it.hasNext())
				TSL.remove(it.next());
			
			lastFun = funActual;
			funActual = null;
			TSactiva = true;
			
			// GCI
			emite("return", new Pair<String,String>("",""), 
					new Pair<String,String>("",""), new Pair<String,String>("",""));
			writeToCIFile("return");
			break;
			
		case 44:
			TSactiva = false;
			funActual = (String) pilaAtrib.get(cima-1).get(0);
			despL = 0;
			
			ArrayList atrib = TSG.get((String) pilaAtrib.get(cima-1).get(0));
			atrib.set(1, "fun");
			atribs.set(4, new ArrayList<String>());
			atrib.set(5, (String) pilaAtrib.get(cima-3).get(1));
			atrib.set(6, nuevaEtiqFun());
			TSG.put((String) pilaAtrib.get(cima-1).get(0), atrib);
			
			atribs.set(5, (String) pilaAtrib.get(cima-3).get(1));
			atribs.set(7, (String) pilaAtrib.get(cima-1).get(0));
			
			nAmbitos++;
			break;
			
		case 45:
			atribs.set(3, (String) pilaAtrib.get(cima-3).get(3));
			atribs.set(4, (ArrayList<String>) pilaAtrib.get(cima-3).get(4));
			
			zonaDecl = false;
			break;
			
		case 46:
			atribs.set(1, (String) pilaAtrib.get(cima-3).get(1));
			atribs.set(5, (String) pilaAtrib.get(cima-3).get(5));
			break;
			
		case 47:
			atribs.set(1, (String) pilaAtrib.get(cima-1).get(1));
			break;
			
		case 48:
			atribs.set(1, "tipo_vacio");
			break;
			
		case 49:
			ArrayList arr1 = new ArrayList();
			for(int i = 0; i < 8; i++)
				arr1.add("");

			arr1.set(1, (String) pilaAtrib.get(cima-5).get(1));
			arr1.set(2, String.valueOf(despL));
			arr1.set(4, new ArrayList<String>());
			TSL.put((String) pilaAtrib.get(cima-3).get(0), arr1);
			despL = despL + Integer.valueOf((String) pilaAtrib.get(cima-5).get(2));
			
			arr = (ArrayList) pilaAtrib.get(cima-1).get(4);
			if(arr.size() == 0) {
				atribs.set(3, "1");
				ArrayList<String> a = new ArrayList<String>();
				a.add((String) pilaAtrib.get(cima-5).get(1));
				atribs.set(4, a);
			}
			else {
				int numParam = Integer.valueOf((String) pilaAtrib.get(cima-1).get(3)) + 1;
				atribs.set(3, String.valueOf(numParam));
				ArrayList<String> params = (ArrayList<String>) pilaAtrib.get(cima-1).get(4);
				params.add(0, (String) pilaAtrib.get(cima-5).get(1));
				atribs.set(4, params);
			}
			break;
				
		case 50:
			atribs.set(4, new ArrayList<String>());
			break;
			
		case 51:
			ArrayList arr2 = new ArrayList();
			for(int i = 0; i < 8; i++)
				arr2.add("");
			
			arr2.set(1, (String) pilaAtrib.get(cima-5).get(1));
			arr2.set(2, String.valueOf(despL));
			arr2.set(4, new ArrayList<String>());
			TSL.put((String) pilaAtrib.get(cima-3).get(0), arr2);
			despL = despL + Integer.valueOf((String) pilaAtrib.get(cima-5).get(2));
			
			arr = (ArrayList) pilaAtrib.get(cima-1).get(4);
			if(arr.size() == 0) {
				atribs.set(3, "1");
				ArrayList<String> a = new ArrayList<String>();
				a.add((String) pilaAtrib.get(cima-5).get(1));
				atribs.set(4, a);
			}
			else {
				int numParam = Integer.valueOf((String) pilaAtrib.get(cima-1).get(3)) + 1;
				atribs.set(3, String.valueOf(numParam));
				ArrayList<String> params = (ArrayList<String>) pilaAtrib.get(cima-1).get(4);
				params.add(0, (String) pilaAtrib.get(cima-5).get(1));
				atribs.set(4, params);
			}
			break;
			
		case 52:
			atribs.set(4, new ArrayList<String>());
			break;
		}
		
		// Se saca el consecuente
		for(int i = 0; i < 2*k; i++) {
			pilaAtrib.pop();
		}
		// Se mete el antecedente
		pilaAtrib.push(atribs);
		// Se mete un hueco para el estado del antecedente
		pilaAtrib.push(new ArrayList<String>());
		
		
		if(nRegla.equals("45")) {
			cima = pilaAtrib.size() - 1;
			id = (String) pilaAtrib.get(cima-3).get(7);
			pilaAtrib.get(cima-1).set(7, id);
			ArrayList atr = TSG.get(id);
			atr.set(3, pilaAtrib.get(cima-1).get(3));
			atr.set(4, pilaAtrib.get(cima-1).get(4));
			TSG.put(id, atr);
			
			// GCI
			emite(":", new Pair<String,String>("etiq", buscarEtiqTS(id)),
					new Pair<String,String>("",""), new Pair<String,String>("",""));
			writeToCIFile(buscarEtiqTS(id) + ":");
		}	
	}
	
	
	
	/* 
	 * Método que implementa el Generador de 
	 * Código Objeto del Compilador.
	 */
	public static void GCO (String op, Pair<String,String> arg1,
							Pair<String,String> arg2, Pair<String,String> res) {
		
		ArrayList<String> arrOp1 = dirOperando(arg1, "R5");
		ArrayList<String> arrOp2 = dirOperando(arg2, "R6");
		ArrayList<String> arrOpRes;		
		if(op.equals("call_ret"))
			arrOpRes = dirOperando(res, "R3");
		else
			arrOpRes = dirOperando(res, "R7");
			
		String op1 = arrOp1.get(0); 
		String op2 = arrOp2.get(0);
		String opRes = arrOpRes.get(0);
		
		String instrOp1 = arrOp1.get(1);
		String instrOp2 = arrOp2.get(1);
		String instrOpRes = arrOpRes.get(1);

		
		String nombreFun = "";
		//String tamVD = "";
		//String instr = ";; \""+op+"\" - op1("+arg1+"), op2("+arg2+"), res("+res+")\n" +
		//		instrOp1 + instrOp2 + instrOpRes;
		String instr = instrOp1 + instrOp2 + instrOpRes;
		
		switch(op) {
			case "+":
				instr += "\t\tADD "+op1+", "+op2+"\n" +
						 "\t\tMOVE .A, "+opRes+"\n";
				break;
				
			case "*":
				instr += "\t\tMUL "+op1+", "+op2+"\n" +
						 "\t\tMOVE .A, "+opRes+"\n";
				break;
				
			case "!":
				String etiq1 = nuevaEtiq();
				String etiq2 = nuevaEtiq();
				instr += "\t\tCMP "+op1+", #0\n" +
						 "\t\tBZ /"+etiq1+"\n" +
						 "\t\tMOVE #0, "+opRes+"\n" +
						 "\t\tBR /"+etiq2+"\n" +
						 etiq1+":\tMOVE #1, "+opRes+"\n" + 
						 etiq2+":\tNOP\n";
				break;
				
			case "=ent":
				instr += "\t\tMOVE "+op1+", "+opRes+"\n";
				break;
				
			case "=cad":
				String desp1 = "", despRes = "";
				String regIndice1 = "", regIndiceRes = "";
				if(arg1.getLeft() == "cte_cad") {
					desp1 = op1;
					regIndice1 = "#0";
				}
				else {
					if(Integer.parseInt(arg1.getRight()) > 127)
						desp1 = "#0";
					else
						desp1 = op1.substring(0, op1.indexOf("["));
					regIndice1 = op1.substring(op1.indexOf("[")+1, op1.length()-1);
				}
				
				if(Integer.parseInt(res.getRight()) > 127)
					despRes = "#0";
				else
					despRes = opRes.substring(0, opRes.indexOf("["));
				regIndiceRes = opRes.substring(opRes.indexOf("[")+1, opRes.length()-1);

				instr += "\t\tADD "+desp1+", "+regIndice1+"\n" +
						 "\t\tMOVE .A, .R8\n" +
						 "\t\tADD "+despRes+", "+regIndiceRes+"\n" +
						 "\t\tMOVE .A, .R9\n" +
						 "bucle"+nBucles+": MOVE [.R8], [.R9]\n" + 
						 "\t\tINC .R8\n" + 
						 "\t\tINC .R9\n" + 
						 "\t\tCMP [.R8], #0\n" + 
						 "\t\tBNZ /bucle"+nBucles+"\n" +
						 "\t\tMOVE #0, [.R9]\n";
				nBucles++;
				break;
				
			case "goto":
				instr += "\t\tBR "+op1+"\n";
				break;
				
			case "goto_==":
				instr += "\t\tCMP "+op1+", "+op2+"\n"+
						 "\t\tBZ "+opRes+"\n";
				break;
				
			case "goto_>":
				instr += "\t\tCMP "+op2+", "+op1+"\n"+
						 "\t\tBN "+opRes+"\n";
				break;
				
			case "param":
				nombreFun = (funActual == null) ? "main" : funActual;
				
				instr += "\t\tADD #tam_RA_"+nombreFun+", .IX\n" +
						 "\t\tADD #"+tamParams+", .A\n" +
						 "\t\tMOVE "+op1+", [.A]\n";
				
				@SuppressWarnings("unchecked") ArrayList<String> tipoParam = (ArrayList<String>) TSG.get(llamandoFun).get(4);
				if (tipoParam.get(nParam-1).equals("cad"))
					tamParams += 64;
				else
					tamParams += 1;
				
				nParam--;
				break;
				
			case "call":
				tamParams = 1;
				
				nombreFun = (funActual == null) ? "main" : funActual;
				
				instr += "\t\t;; SECUENCIA DE LLAMADA\n" +
						 "\t\t; se almacena la direccion de retorno\n" +
						 "\t\tADD #tam_RA_"+nombreFun+", .IX\n" +
						 "\t\tMOVE #dir_ret"+nDirRet+", [.A]\n" +
						 "\t\t; se incrementa el Puntero de Pila\n" +
						 "\t\tADD #tam_RA_"+nombreFun+", .IX\n" +
						 "\t\tMOVE .A, .IX\n" + 
						 "\t\t; se salta al procedimiento llamado\n" +
						 "\t\tBR "+op1+"\n" +
						 "\n\t\t;; SECUENCIA DE RETORNO\n" +
						 "\t\t; se decrementa el Puntero de Pila\n" +
						 "dir_ret"+nDirRet+": SUB .IX, #tam_RA_"+nombreFun+"\n" +
						 "\t\tMOVE .A, .IX\n";
				nDirRet++;
				break;
				
			case "call_ret":
				tamParams = 1;
				
				String nombreRALlamador = (funActual == null) ? "main" : funActual;
				String nombreRALlamado = buscarNombreFun(op1.substring(1));
				
				instr += "\t\t;; SECUENCIA DE LLAMADA\n" +
						 "\t\t; se almacena la direccion de retorno\n" +
						 "\t\tADD #tam_RA_"+nombreRALlamador+", .IX\n" +
						 "\t\tMOVE #dir_ret"+nDirRet+", [.A]\n" +
						 "\t\t; se incrementa el Puntero de Pila\n" +
						 "\t\tADD #tam_RA_"+nombreRALlamador+", .IX\n" +
						 "\t\tMOVE .A, .IX\n" + 
						 "\t\t; se salta al procedimiento llamado\n" +
						 "\t\tBR "+op1+"\n" +
						 "\n\t\t;; SECUENCIA DE RETORNO\n";
				
				if(TSG.get(nombreRALlamado).get(5).equals("cad")) {
					String desp = "", regIndice = "";
					if(res.getLeft() == "cte_cad") {
						desp = opRes;
						regIndice = "#0";
					}
					else {
						if(Integer.parseInt(res.getRight()) > 127)
							desp = "#0";
						else
							desp = opRes.substring(0, opRes.indexOf("["));
						regIndice = opRes.substring(opRes.indexOf("[")+1, opRes.length()-1);
					}
										
					instr += "\t\t; se guarda el valor devuelto\n" + 
							 "dir_ret"+nDirRet+": SUB #tam_RA_"+nombreRALlamado+", #64\n" +
							 "\t\tADD .A, .IX\n" +
							 "\t\tMOVE .A, .R8\n" +
							 "\t\tADD "+desp+", "+regIndice+"\n" +
							 "\t\tMOVE .A, .R9\n" +
							 "bucle"+nBucles+": MOVE [.R8], [.R9]\n" + 
							 "\t\tINC .R8\n" + 
							 "\t\tINC .R9\n" + 
							 "\t\tCMP [.R8], #0\n" + 
							 "\t\tBNZ /bucle"+nBucles+"\n" +
							 "\t\t; se decrementa el Puntero de Pila\n" +
							 "\t\tSUB .IX, #tam_RA_"+nombreRALlamador+"\n" +
							 "\t\tMOVE .A, .IX\n";
					nBucles++;
				}
				
				else {			
					instr += "\t\t; se recoge el valor devuelto\n" +
							 "dir_ret"+nDirRet+": SUB #tam_RA_"+nombreRALlamado+", #1\n" +
							 "\t\tADD .A, .IX\n" +
							 "\t\tMOVE [.A], .R9\n" +
							 "\t\t; se decrementa el Puntero de Pila\n" +
							 "\t\tSUB .IX, #tam_RA_"+nombreRALlamador+"\n" +
							 "\t\tMOVE .A, .IX\n" +
							 "\t\t; se guarda el valor devuelto\n" +
							 "\t\tMOVE .R9, "+opRes+"\n";
				}
				nDirRet++;
				break;
				
			case "return":				
				if(res.getRight() == "") {
					// Se añade el tamaño del RA del procedimiento
					if(funActual == null && !lastFun.equals("main"))
						putTamRA(lastFun);

					writeToCOFile("\t\tBR [.IX]\n");
					return;
				}
				else {					
					nombreFun = funActual;
					if(TSG.get(nombreFun).get(5).equals("cad")) {
						String desp = "", regIndice = "";
						if(res.getLeft() == "cte_cad") {
							desp = opRes;
							regIndice = "#0";
						}
						else {
							if(Integer.parseInt(res.getRight()) > 127)
								desp = "#0";
							else
								desp = opRes.substring(0, opRes.indexOf("["));
							regIndice = opRes.substring(opRes.indexOf("[")+1, opRes.length()-1);
						}
						
						instr += "\t\tSUB #tam_RA_"+nombreFun+", #64\n" +
								 "\t\tADD .A, .IX\n" +
								 "\t\tMOVE .A, .R9\n" +
								 "\t\tADD "+desp+", "+regIndice+"\n" +
								 "\t\tMOVE .A, .R8\n" +
								 "bucle"+nBucles+": MOVE [.R8], [.R9]\n" + 
								 "\t\tINC .R8\n" + 
								 "\t\tINC .R9\n" + 
								 "\t\tCMP [.R8], #0\n" + 
								 "\t\tBNZ /bucle"+nBucles+"\n" +
								 "\t\tBR [.IX]\n";
						nBucles++;						
					}
					else {
						instr += "\t\tSUB #tam_RA_"+nombreFun+", #1\n" + 
								 "\t\tADD .A, .IX\n" +
								 "\t\tMOVE "+opRes+", [.A]\n" + 
								 "\t\tBR [.IX]\n";
					}
				}	
				break;
				
			case ":":
				op1 = op1.substring(1);

				if(op1.startsWith("EtFun"))
					instr += "\n;;;;;;;;;;; FUNCIÓN '"+funActual+"' ;;;;;;;;;;;\n\n";
				
				instr += op1+":\tNOP\n";
				break;
				
			case "input_ent":
				instr += "\t\tININT "+opRes+"\n";
			
				break;
			
			case "input_cad":
				instr += "\t\tINSTR "+opRes+"\n";
				break;
				
			case "alert_ent":
				instr += "\t\tWRINT "+opRes+"\n";
				break;
				
			case "alert_cad":
				instr += "\t\tWRSTR "+opRes+"\n";
				break;	
		}
				
		// Se escriben las instrucciones en el fichero
		if(!TSactiva)
			writeToCOFile(instr);
		else {
			writeToTempCOFile(instr);
		}
	}
	
	
	
	/*
	 * Método que implementa el Gestor de Errores
	 * del Compilador.
	 * Recibe el nombre del Analizador que lo llama
	 * y el número de error e imprime por pantalla
	 * y escribe en el fichero de errores el mensaje
	 * de error asociado al número de error.
	 */
	@SuppressWarnings("unchecked")
	private static void GestorErrores (String analizador, int nError, String token, String extra) {
		
		String msg;
		int linea = lineaActual;
				
		if(analizador.equals("AnLex"))
			msg = "Error léxico en línea " + linea + ": ";
		
		else if(analizador.equals("AnSt")) {
			msg = "Error sintáctico en línea " + linea + ": ";
			linea = linea - nLineas;
		}
		
		else {
			msg = "Error semántico en línea " + linea + ": ";
			linea = linea - nLineas;
		}
	
		// Selección del mensaje de error
		try {
			switch(nError) {
			case 1:
				FErr.write(msg + "Token '" + token + "' no esperado.\n");
				break;
				
			case 2:
				FErr.write(msg + "Entero fuera de rango. El máximo entero permitido es el 32767.\n");
				break;
				
			case 3:
				FErr.write(msg + "Cadena con más de 64 caracteres.\n");
				break;
			
			case 4:
				FErr.write(msg + "Variable '" + token + "' ya declarada.\n");
				break;
			
			case 5:
				FErr.write(msg + "La condición de la sentencia 'if' debe ser " +
							"de tipo lógico.\n");
				break;
				
			case 6:
				ArrayList<String> arg = (ArrayList<String>) TSG.get(token).get(4);
				String args = "(";

				if(arg.size() != 0) {
					String ar = "";
					
					ar = arg.get(0);
					if(ar.equals("ent"))
						ar = "number";
					else if(ar.equals("log"))
						ar = "boolean";
					else if(ar.equals("cad"))
						ar = "string";
					args = args + ar;
					
					for(int i = 1; i < arg.size(); i++) {
						ar = arg.get(i);
						if(ar.equals("ent"))
							ar = "number";
						else if(ar.equals("log"))
							ar = "boolean";
						else if(ar.equals("cad"))
							ar = "string";
						if(i < arg.size())
						args = args + ", " + ar; 
					}
				}
				args = args + ")";

				FErr.write(msg + "La llamada al método " + token + args +
							" es incorrecta.\n");
				break;
				
			case 7:
				FErr.write(msg + "Los tipos de ambos lados de la asignación " + 
							"deben ser iguales.\n");
				break;
				
			case 8:
				FErr.write(msg + "La sentencia 'alert' solo admite argumentos " + 
							"de tipo entero o cadena.\n");
				break;
				
			case 9:
				FErr.write(msg + "La sentencia 'input' solo admite argumentos " + 
							"de tipo entero o cadena.\n");
				break;
				
			case 10:
				FErr.write(msg + "El operador '>' solo se aplica sobre " + 
							"datos de tipo entero.\n");
				break;
				
			case 11:
				FErr.write(msg + "El operador '+' solo se aplica sobre " + 
							"datos de tipo entero.\n");
				break;
				
			case 12:
				FErr.write(msg + "El operador '!' solo se aplica sobre " + 
							"datos de tipo lógico.\n");
				break;
				
			case 13:
				FErr.write(msg + "El método '" + token + "' no recibe argumentos.\n");
				break;
				
			case 14:
				FErr.write(msg + "El operador '++' solo se aplica sobre " + 
							"datos de tipo entero.\n");
				break;
				
			case 15:
				FErr.write(msg + "Método '" + token + "' no declarado.\n");
				break;
				
			case 16:
				FErr.write(msg + "El método '" + token + "' no puede devolver " + 
						"ningún valor.\n");
				break;
				
			case 17:
				FErr.write(msg + "El tipo de la sentencia 'return' no coincide " + 
							"con el tipo de retorno del método '" + token + "': " + extra + ".\n");
				break;
				
			case 18:
				FErr.write(msg + "Se esperaba un tipo (number, boolean o string). " +
						"La forma de declarar una variable es: let tipo nombre [= expresión];.\n");
				break;
				
			case 19:
				FErr.write(msg + "Se esperaba el nombre de una variable. " +
						"La forma de declarar una variable es: let tipo nombre [= expresión];.\n");
				break;
				
			case 20:
				FErr.write(msg + "Se esperaba una expresión después de '='.\n");
				break;
				
			case 21:
				FErr.write(msg + "Se esperaba '('. La forma de utilizar una sentencia condicional es: " +
						"if (condición) {cuerpo1} [else {cuerpo2}].\n");
				break;
				
			case 22:
				FErr.write(msg + "Se esperaba una expresión después de '('. La forma de utilizar una sentencia " +
						"condicional es: if (condición) {cuerpo1} [else {cuerpo2}].\n");
				break;

			case 23:
				FErr.write(msg + "Se esperaba ')'. La forma de utilizar una sentencia condicional es: " +
						"if (condición) {cuerpo1} [else {cuerpo2}].\n");
				break;
				
			case 24:
				FErr.write(msg + "Se esperaba '{'. La forma de utilizar una sentencia condicional es: " +
						"if (condición) {cuerpo1} [else {cuerpo2}].\n");
				break;
				
			case 25:
				FErr.write(msg + "Se esperaba '}'. La forma de utilizar una sentencia condicional es: " +
						"if (condición) {cuerpo1} [else {cuerpo2}].\n");
				break;
				
			case 26:
				FErr.write(msg + "Se esperaba '('. La forma de utilizar la instrucción " + 
						"'alert' es: alert (expresión);.\n");
				break;
				
			case 27:
				FErr.write(msg + "Se esperaba una expresión. La forma de utilizar la instrucción " + 
						"'alert' es: alert (expresión);.\n");
				break;
				
			case 28:
				FErr.write(msg + "Se esperaba ')'. La forma de utilizar la instrucción " + 
						"'alert' es: alert (expresión);.\n");
				break;
				
			case 29:
				FErr.write(msg + "Se esperaba '('. La forma de utilizar la instrucción " + 
						"'input' es: input (variable);.\n");
				break;
				
			case 30:
				FErr.write(msg + "Se esperaba el nombre de una variable. La forma de utilizar " + 
						"la instrucción 'input' es: input (variable);.\n");
				break;
				
			case 31:
				FErr.write(msg + "Se esperaba ')'. La forma de utilizar la instrucción " + 
						"'input' es: input (variable);.\n");
				break;
				
			case 32:
				FErr.write(msg + "Se esperaba el nombre de la función. La forma de " +
						"declarar una función es: function [tipo] nombre ([argumentos]) {cuerpo}.\n");
				break;
				
			case 33:
				FErr.write(msg + "Se esperaba '('. La forma de declarar una función es: " +
						"function [tipo] nombre ([argumentos]) {cuerpo}.\n");
				break;
				
			case 34:
				FErr.write(msg + "Se esperaba ')'. La forma de declarar una función es: " +
						"function [tipo] nombre ([argumentos]) {cuerpo}.\n");
				break;
				
			case 35:
				FErr.write(msg + "Se esperaba '{'. La forma de declarar una función es: " +
						"function [tipo] nombre ([argumentos]) {cuerpo}.\n");
				break;
				
			case 36:
				FErr.write(msg + "Se esperaba '}'. La forma de declarar una función es: " +
						"function [tipo] nombre ([argumentos]) {cuerpo}.\n");
				break;
				
			case 37:
				FErr.write(msg + "Se esperaba una expresión después de '!'.\n");
				break;
				
			case 38:
				FErr.write(msg + "Se esperaba una expresión después de '('.\n");
				break;
				
			case 39:
				FErr.write(msg + "Se esperaba el nombre de una variable.\n");
				break;
				
			case 40:
				FErr.write(msg + "Falta ')' para completar la llamada a la función.\n");
				break;
				
			case 41:
				FErr.write(msg + "Se esperaba ';' para completar la llamada a la función.\n");
				break;
				
			case 42:
				FErr.write(msg + "Se esperaba ';' para completar la sentencia/expresión.\n");
				break;
				
			case 43:
				FErr.write(msg + "No está permitido declarar un método dentro de otro.\n");
				break;
				
			case 44:
				FErr.write(msg + "Se esperaba '\"' para cerrar la cadena.\n");
				break;
				
			case 45:
				FErr.write(msg + "Solo se admiten comentarios del tipo /* Comentario */.\n");
				break;
			
			case 46:
				FErr.write(msg + "No se permite el uso del operador '++' con funciones.\n");
				break;
			}
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	
	
	/*
	 * Método que busca el número de error correspondiente al 
	 * estado del AFD del Analizador Sintáctico y
	 * al último token recibido, y llama al 
	 * Gestor de Errores con dicho número de error.
	 */
	private static void buscarError (String estado, String token) {
		
		int i = Integer.valueOf(estado);	
		
				
		if(i == 4) {
			GestorErrores("AnSt", 18, "", "");
			return;
		}
		
		else if(i == 15) {
			GestorErrores("AnSt", 19, "", "");
			return;
		}
		
		else if(i == 24) {
			GestorErrores("AnSt", 20, token, "");
			return;
		}
		
		else if(i == 5) {
			GestorErrores("AnSt", 21, "", "");
			return;
		}
		
		else if(i == 19) {
			GestorErrores("AnSt", 22, "", "");
			return;
		}
		
		else if(i == 61 || i == 93) {
			GestorErrores("AnSt", 24, "", "");
			return;
		}
		
		else if(i == 86) {
			GestorErrores("AnSt", 25, "", "");
			return;
		}
		
		else if(i == 7) {
			GestorErrores("AnSt", 26, "", "");
			return;
		}
		
		else if(i == 20) {
			GestorErrores("AnSt", 27, "", "");
			return;
		}
		
		else if((i == 33 || i == 34 || i == 42 || i == 53)
				&& token.equals("alert")) {
				GestorErrores("AnSt", 28, "", "");
				return;
		}
		
		else if(i == 8) {
			GestorErrores("AnSt", 29, "", "");
			return;
		}
		
		else if(i == 21) {
			GestorErrores("AnSt", 30, "", "");
			return;
		}
		
		else if(i == 43) {
			GestorErrores("AnSt", 31, "", "");
			return;
		}
		
		else if(i == 38) {
			GestorErrores("AnSt", 32, "", "");
			return;
		}
		
		else if(i == 11 || i == 58) {
			GestorErrores("AnSt", 33, "", "");
			return;
		}
		
		else if(i == 74 || i == 84 || i == 91 || i == 94) {
			GestorErrores("AnSt", 34, "", "");
			return;
		}
		
		else if(i == 36 || i == 73) {
			GestorErrores("AnSt", 35, "", "");
			return;
		}
		
		else if(i == 83) {
			GestorErrores("AnSt", 36, "", "");
			return;
		}
		
		else if(i == 29) {
			GestorErrores("AnSt", 37, "", "");
			return;
		}
		
		else if(i == 31) {
			GestorErrores("AnSt", 38, "", "");
			return;
		}
				
		else if(i == 16 || i == 17 || i == 18 || i == 32 
			|| i == 39 || i == 57 || i == 89) {
			GestorErrores("AnSt", 39, "", "");
			return;
		}
		
		else if(i == 45 || i == 46 || i == 81) {
			GestorErrores("AnSt", 40, "", "");
			return;
		}
		
		else if(i == 22 || i == 59 || i == 64) {
			GestorErrores("AnSt", 41, "", "");
			return;
		}
		
		else if(i == 25 || i == 40 || i == 62 || i == 63) {
			GestorErrores("AnSt", 42, "", "");
			return;
		}
		
		else {
			GestorErrores("AnSt", 1, token, "");
			return;
		}
	}
	
	
	
	/*
	 *  Función que devuelve el terminal asociado 
	 *  al token tok.
	*/
	private static String tokenTransformer (Pair<String, String> tok) {
		
		String tipo = tok.getLeft();
		
		if(tipo.equals("PalRes"))
			return TPalRes[Integer.parseInt(tok.getRight())];

		else if(tipo.equals("OpArit"))
			return OpArit[Integer.parseInt(tok.getRight())];
		
		else if(tipo.equals("OpRel"))
			return OpRel[Integer.parseInt(tok.getRight())];
		
		else if(tipo.equals("OpLog"))
			return OpLog[Integer.parseInt(tok.getRight())];
		
		else if(tipo.equals("preInc"))
			return "++";
		
		else if(tipo.equals("igual"))
			return "=";
		
		else if(tipo.equals("coma"))
			return ",";
		
		else if(tipo.equals("ptoComa"))
			return ";";
		
		else if(tipo.equals("parentAbre"))
			return "(";
		
		else if(tipo.equals("parentCierra"))
			return ")";
		
		else if(tipo.equals("llaveAbre"))
			return "{";
		
		else if(tipo.equals("llaveCierra"))
			return "}";
		
		else if(tipo.equals("igual"))
			return "=";
		
		else if(tipo.equals("cteEntera"))
			return "ent";
		
		else if(tipo.equals("cadena"))
			return "cad";
		
		else if(tipo.equals("EOF"))
			return "$";
	
		else
			return "id";
	}
	
	
	
	/*
	 *  Función busca en la tabla de Palabras 
	 *  Reservadas. Si encuentra la palabra reservada
	 *  "lex" devuelve su posicion en dicha tabla, o
	 *  -1 en o.c.
	*/
	private static int buscarTPalRes(String lex) {
		
		boolean found = false;
		int i = 1;
		while(!found && i < TPalRes.length) {
			if(TPalRes[i].equals(lex))
				found = true;
			i++;
		}
		
		if(found)
			return i-1;
		else
			return -1;
	}
	
	
	/*
	 * Función que crea una nueva etiqueta.
	 */
	private static String nuevaEtiq () {
		return "Etiq" + nEtiq++;
	}
	
	
	/*
	 * Función que crea una nueva etiqueta para
	 * una función.
	 */
	private static String nuevaEtiqFun () {
		return "EtFun" + nAmbitos;
	}
	
	
	
	/*
	 *  Función que crea un token a partir de los argumentos
	 *  y añade dicho token al fichero de tokens, y al fichero
	 *  de Tabla de Simbolos si se trata de un identifiador.
	*/
	private static Pair<String, String> GenToken (String tipo, String atrib) {
		
		String token = "<" + tipo + ", " + atrib + ">";

		// Se escribe el token en el fichero de tokens
	    try {
	    	FTokens.write(token + "\n");
	    }
	    catch (IOException e) {
	    	System.err.println("An error occurred.");
	    	e.printStackTrace();
	     } 
	   
		return new Pair<String, String>(tipo, atrib);
	}
	
	
	
	/*
	 * Método que escribe la información de las
	 * Tablas de Símbolos en el fichero TS.txt
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void writeToTS () {
		
		ArrayList atribs;
		String lex;
		Map<String, ArrayList> ts;
		Iterator it;
		
    	try {
    		for(int i = 0; i < bTS.size(); i++) {
    			ts = bTS.get(i).getRight();
    			it = ts.keys().iterator();

    			if(i == 0)
    				FTS.write("TABLA DE SIMBOLOS GLOBAL #1:\n");
    			else
    				FTS.write("TABLA DE SIMBOLOS DE " +  bTS.get(i).getLeft() + 
    							" #" + (i+1) + ":\n");
    		
	    		while(it.hasNext()) {
	    			lex = (String) it.next();
					atribs = ts.get(lex);
	
		    		FTS.write("\n*LEXEMA: '" + lex + "'\n");
		    		FTS.write("\tATRIBUTOS: \n");
		    		for(int j = 1; j < 7; j++) {
		    			switch(j) {
		    			case 1:
		    				if(!atribs.get(1).equals(""))
		    					FTS.write("\t+tipo:\t\t\t'" + atribs.get(1) + "'\n");
				    		break;
				    		
		    			case 2:
		    				if(!atribs.get(2).equals(""))
		    					FTS.write("\t+despl:\t\t\t" + atribs.get(2) + "\n");
				    		break;
				    		
		    			case 3:
		    				if(!atribs.get(3).equals(""))
		    					FTS.write("\t+numParam:\t\t" + atribs.get(3) + "\n");
				    		break;
				    		
		    			case 4:
		    				ArrayList<String> tipoParam = (ArrayList<String>) atribs.get(4);
		    				if(tipoParam.size() != 0) {
		    					for(int k = 0; k < tipoParam.size(); k++)
		        					FTS.write("\t+tipoParam" + 0+(k+1) + ":\t'" + tipoParam.get(k) + "'\n");    						
		    				}
				    		break;
				    		
		    			case 5:
		    				if(!atribs.get(5).equals("")) 
		    					FTS.write("\t+tipoRet:\t\t'" + atribs.get(5) + "'\n");
				    		break;
				    		
		    			case 6:
		    				if(!atribs.get(6).equals(""))
		    					FTS.write("\t+etiqFun:\t\t'" + atribs.get(6) + "'\n");
				    		break;
				    	}
		    		}
	    		}
	    		if(i < bTS.size()-1)
	    			FTS.write("\n--------------------------------------\n");
    		}
    	} catch (IOException e) { e.printStackTrace(); }
	}
	
	

	/*
	 *  Método que se encarga del llenado de tablas,
	 *  Matriz de Transición del Autómata para el AnLex
	 *  y Tabla Acción-GoTo del AnSt del Procesador.
	 */
	private static void llenadoTablas() {
		
		// Palabras Reservadas
		TPalRes[1] = "boolean";
		TPalRes[2] = "number";
		TPalRes[3] = "string";
		TPalRes[4] = "alert";
		TPalRes[5] = "input";
		TPalRes[6] = "function";
		TPalRes[7] = "if";
		TPalRes[8] = "else";
		TPalRes[9] = "return";
		TPalRes[10] = "let";
		
		// Operadores Aritméticos
		OpArit[1] = "+";

		// Operadores Relacionales
		OpRel[1] = ">";
		
		// Operadores Lógicos
		OpLog[1] = "!";
		
		// Reglas
		reglas[1] = "1A1"; reglas[2] = "2A"; reglas[3] = "2A";
		reglas[4] = "0A"; reglas[5] = "5B"; reglas[6] = "8B";
		reglas[7] = "4W"; reglas[8] = "0W"; reglas[9] = "1B";
		reglas[10] = "2B1"; reglas[11] = "0B1"; reglas[12] = "2C";
		reglas[13] = "0C"; reglas[14] = "1T"; reglas[15] = "1T";
		reglas[16] = "1T"; reglas[17] = "3S"; reglas[18] = "3N";
		reglas[19] = "2N"; reglas[20] = "3S"; reglas[21] = "5S";
		reglas[22] = "5S"; reglas[23] = "1R"; reglas[24] = "0R";
		reglas[25] = "3E"; reglas[26] = "1E"; reglas[27] = "3E1";
		reglas[28] = "1E1"; reglas[29] = "2E2"; reglas[30] = "1E2";
		reglas[31] = "3E3"; reglas[32] = "3E3"; reglas[33] = "0Q";
		reglas[34] = "3Q"; reglas[35] = "1E3"; reglas[36] = "1E3";
		reglas[37] = "2L"; reglas[38] = "0L"; reglas[39] = "3L1";
		reglas[40] = "0L1"; reglas[41] = "1U"; reglas[42] = "0U";
		reglas[43] = "3F"; reglas[44] = "3F1"; reglas[45] = "3F2";
		reglas[46] = "3F3"; reglas[47] = "1H"; reglas[48] = "0H";
		reglas[49] = "3P"; reglas[50] = "0P"; reglas[51] = "4P1";
		reglas[52] = "0P1";

		
		// Matriz de Transición del AFD
		
			/* Estado 0 */ 
		MT_AFD.addValor(0, '\t', new Pair<Integer, String>(0, ""));
		MT_AFD.addValor(0, 32, new Pair<Integer, String>(0, "")); //32 - blanco
		MT_AFD.addValor(0, '\n', new Pair<Integer, String>(0, ""));
		MT_AFD.addValor(0, '\r', new Pair<Integer, String>(0, ""));
		MT_AFD.addValor(0, 0, new Pair<Integer, String>(0, "")); //0 - nulo

			//letras
		for (int i = 'A'; i <= 'z'; i++) {
				if((i >= 'A' && i <= 'Z') || (i >= 'a' && i <='z'))
					MT_AFD.addValor(0, i, new Pair<Integer, String>(1, "A"));
		}
			// digitos
		for(int i = '0'; i <= '9'; i++) {
			MT_AFD.addValor(0, i, new Pair<Integer, String>(2, "B"));
		}
		
		MT_AFD.addValor(0, '"', new Pair<Integer, String>(3, ""));
		MT_AFD.addValor(0, '+', new Pair<Integer, String>(4, ""));
		MT_AFD.addValor(0, '/', new Pair<Integer, String>(5, ""));
		MT_AFD.addValor(0, '=', new Pair<Integer, String>(13, "H"));
		MT_AFD.addValor(0, ',', new Pair<Integer, String>(14, "I"));
		MT_AFD.addValor(0, ';', new Pair<Integer, String>(15, "J"));
		MT_AFD.addValor(0, '(', new Pair<Integer, String>(16, "K"));
		MT_AFD.addValor(0, ')', new Pair<Integer, String>(17, "L"));
		MT_AFD.addValor(0, '{', new Pair<Integer, String>(18, "M"));
		MT_AFD.addValor(0, '}', new Pair<Integer, String>(19, "N"));
		MT_AFD.addValor(0, '>', new Pair<Integer, String>(20, "O"));
		MT_AFD.addValor(0, '!', new Pair<Integer, String>(21, "P"));

		
			/* Estado 1 */
			// letras
		for (int i = 'A'; i <= 'z'; i++) {
			if((i >= 'A' && i <= 'Z') || (i >= 'a' && i <='z'))
				MT_AFD.addValor(1, i, new Pair<Integer, String>(1, "A"));
		}
			// digitos
		for(int i = '0'; i <= '9'; i++) {
			MT_AFD.addValor(1, i, new Pair<Integer, String>(1, "A"));
		}
		MT_AFD.addValor(1, '_', new Pair<Integer, String>(1, "A"));
			// oc
		for (int i = 0; i <= 125; i++) {
			if((i >= 0 && i <= '/') || (i >= ':' && i <='@') 
					|| (i >= '[' && i <= 94	) || (i >= '{' && i <= '}'))
				MT_AFD.addValor(1, i, new Pair<Integer, String>(8, "C"));
		}


		
			/* Estado 2 */
			// digitos
		for(int i = '0'; i <= '9'; i++) {
			MT_AFD.addValor(2, i, new Pair<Integer, String>(2, "B"));
		}
			// oc
		for (int i = 0; i <= 125; i++) {
			if((i >= 0 && i <= '/') || (i >= ':' && i <='}'))
				MT_AFD.addValor(2, i, new Pair<Integer, String>(9, "D"));
		}

		
			/* Estado 3 */
		MT_AFD.addValor(3, '"', new Pair<Integer, String>(10, "E"));
			// c
		for (int i = 32; i <= 254; i++) {
			if((i >= 32 && i <= '!') || (i >= '#' && i <= 254))
				MT_AFD.addValor(3, i, new Pair<Integer, String>(3, "A"));
		}
		
			/* Estado 4 */
		MT_AFD.addValor(4, '+', new Pair<Integer, String>(11, "F"));
			// oc
		for (int i = 0; i <= 125; i++) {
			if((i >= 0 && i <= '*') || (i >= ',' && i <= '}'))
				MT_AFD.addValor(4, i, new Pair<Integer, String>(12, "G"));
		}

			/* Estado 5 */
		MT_AFD.addValor(5, '*', new Pair<Integer, String>(6, ""));
		
		
			/* Estado 6 */
		MT_AFD.addValor(6, '*', new Pair<Integer, String>(7, ""));
		MT_AFD.addValor(6, '\t', new Pair<Integer, String>(6, ""));
		MT_AFD.addValor(6, '\n', new Pair<Integer, String>(6, ""));
		MT_AFD.addValor(6, '\r', new Pair<Integer, String>(6, ""));
			// m
		for (int i = 32; i <= 254; i++) {
			if((i >= 32 && i <= ')') || (i >= '+' && i <= 254))
				MT_AFD.addValor(6, i, new Pair<Integer, String>(6, ""));
		}
		
			/* Estado 7 */
		MT_AFD.addValor(7, '/', new Pair<Integer, String>(0, ""));
		MT_AFD.addValor(7, '*', new Pair<Integer, String>(7, ""));
		MT_AFD.addValor(7, '\t', new Pair<Integer, String>(6, ""));
		MT_AFD.addValor(7, '\n', new Pair<Integer, String>(6, ""));
		MT_AFD.addValor(7, '\r', new Pair<Integer, String>(6, ""));
			// m
		for (int i = 32; i <= 254; i++) {
			if((i >= 32 && i <= ')') || (i >= '+' && i <= 46)
				|| (i >= 48 && i <= 254))
				MT_AFD.addValor(7, i, new Pair<Integer, String>(6, ""));
		}
		
		
		// Tabla Acción y GoTo
		/* Estado 0*/
		TAcc.addValor(0, new String[] {"let", "d4", "if", "d5",
									   "alert", "d7", "input", "d8",
									   "id", "d9", "return", "d10",
									   "function", "d12", "$", "r4",
									   "A", "1", "B", "2", "F", "3",
									   "S", "6", "F1", "11"});

		/* Estado 1*/
		TAcc.addValor(1, new String[] {"$", "a"});
		
		/* Estado 2*/
		TAcc.addValor(2, new String[] {"let", "d4", "if", "d5",
									   "alert", "d7", "input", "d8",
									   "id", "d9", "return", "d10",
									   "function", "d12", "$", "r4",
									   "A", "13", "B", "2", "F", "3",
									   "S", "6", "F1", "11"});

		/* Estado 3*/
		TAcc.addValor(3, new String[] {"let", "d4", "if", "d5",
									   "alert", "d7", "input", "d8",
									   "id", "d9", "return", "d10",
									   "function", "d12", "$", "r4",
									   "A", "14", "B", "2", "F", "3",
									   "S", "6", "F1", "11"});
		
		/* Estado 4*/
		TAcc.addValor(4, new String[] {"number", "d16", "string", "d17",
									   "boolean", "d18", "T", "15"});
		
		/* Estado 5*/
		TAcc.addValor(5, new String[] {"(", "d19"});
		
		/* Estado 6*/
		TAcc.addValor(6, new String[] {"let", "r9", "if", "r9",
									   "alert", "r9", "input", "r9",
									   "id", "r9", "return", "r9",
									   "function", "r9", "$", "r9",
									   "}", "r9"});
		
		/* Estado 7*/
		TAcc.addValor(7, new String[] {"(", "d20"});
		
		/* Estado 8*/
		TAcc.addValor(8, new String[] {"(", "d21"});
		
		/* Estado 9*/
		TAcc.addValor(9, new String[] {"(", "d23", "=", "d24",
									   "N", "22"});
		
		/* Estado 10*/
		TAcc.addValor(10, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", ";", "r24", "id", "r42",
										"R", "25", "E", "26", "E1", "27",
										"E2", "28", "E3", "30", "U", "32"});
		
		/* Estado 11*/
		TAcc.addValor(11, new String[] {"(", "d37", "F2", "36"});
		
		/* Estado 12*/
		TAcc.addValor(12, new String[] {"number", "d16", "string", "d17",
									   "boolean", "d18", "id", "r48",
									   "H", "38", "T", "39"});
		
		/* Estado 13*/
		TAcc.addValor(13, new String[] {"$", "r2"});
		
		/* Estado 14*/
		TAcc.addValor(14, new String[] {"$", "r3"});
		
		/* Estado 15*/
		TAcc.addValor(15, new String[] {"id", "d40"});
		
		/* Estado 16*/
		TAcc.addValor(16, new String[] {"id", "r14"});
		
		/* Estado 17*/
		TAcc.addValor(17, new String[] {"id", "r15"});
		
		/* Estado 18*/
		TAcc.addValor(18, new String[] {"id", "r16"});
		
		/* Estado 19*/
		TAcc.addValor(19, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E", "41", "E1", "27", "E2", "28",
										"E3", "30", "U", "32"});
		
		/* Estado 20*/
		TAcc.addValor(20, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E", "42", "E1", "27", "E2", "28",
										"E3", "30", "U", "32"});
		
		/* Estado 21*/
		TAcc.addValor(21, new String[] {"id", "d43"});
		
		/* Estado 22*/
		TAcc.addValor(22, new String[] {";", "d44"});
		
		/* Estado 23*/
		TAcc.addValor(23, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42", 
										")", "r38",
										"L", "45", "E", "46", "E1", "27",
										"E2", "28", "E3", "30", "U", "32"});
		
		/* Estado 24*/
		TAcc.addValor(24, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E", "47", "E1", "27", "E2", "28",
										"E3", "30", "U", "32"});
		
		/* Estado 25*/
		TAcc.addValor(25, new String[] {";", "d48"});
		
		/* Estado 26*/
		TAcc.addValor(26, new String[] {">", "d49", ";", "r23"});
		
		/* Estado 27*/
		TAcc.addValor(27, new String[] {"+", "d50", ")", "r26",
										";", "r26", ">", "r26",
										",", "r26"});
		
		/* Estado 28*/
		TAcc.addValor(28, new String[] {"+", "r28", ")", "r28",
										";", "r28", ">", "r28",
										",", "r28"});
		
		/* Estado 29*/
		TAcc.addValor(29, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E2", "51", "E3", "30", "U", "32"});

		/* Estado 30*/
		TAcc.addValor(30, new String[] {"+", "r30", ")", "r30",
										";", "r30", ">", "r30",
										",", "r30"});
		
		/* Estado 31*/
		TAcc.addValor(31, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E", "52", "E1", "27",
										"E2", "28", "E3", "30", "U", "32"});

		/* Estado 32*/
		TAcc.addValor(32, new String[] {"id", "d53"});
		
		/* Estado 33*/
		TAcc.addValor(33, new String[] {"+", "r35", ")", "r35",
										";", "r35", ">", "r35",
										",", "r35"});
		
		/* Estado 34*/
		TAcc.addValor(34, new String[] {"+", "r36", ")", "r36",
										";", "r36", ">", "r36",
										",", "r36"});
		
		/* Estado 35*/
		TAcc.addValor(35, new String[] {"id", "r41"});
		
		/* Estado 36*/
		TAcc.addValor(36, new String[] {"{", "d55", "F3", "54"});
		
		/* Estado 37*/
		TAcc.addValor(37, new String[] {"number", "d16", "string", "d17",
									   "boolean", "d18", ")", "r50",
									   "P", "56", "T", "57"});
		
		/* Estado 38*/
		TAcc.addValor(38, new String[] {"id", "d58"});
		
		/* Estado 39*/
		TAcc.addValor(39, new String[] {"id", "r47"});
		
		/* Estado 40*/
		TAcc.addValor(40, new String[] {"=", "d60", ";", "r11",
										"B1", "59"});
		
		/* Estado 41*/
		TAcc.addValor(41, new String[] {")", "d61", ">", "d49"});
		
		/* Estado 42*/
		TAcc.addValor(42, new String[] {")", "d62", ">", "d49"});
		
		/* Estado 43*/
		TAcc.addValor(43, new String[] {")", "d63"});
		
		/* Estado 44*/
		TAcc.addValor(44, new String[] {"let", "r17", "if", "r17",
									   "alert", "r17", "input", "r17",
									   "id", "r17", "return", "r17",
									   "function", "r17", "$", "r17",
									   "}", "r17"});
		
		/* Estado 45*/
		TAcc.addValor(45, new String[] {")", "d64"});
		
		/* Estado 46*/
		TAcc.addValor(46, new String[] {",", "d66", ">", "d49",
										")", "r40", "L1", "65"});
		
		/* Estado 47*/
		TAcc.addValor(47, new String[] {">", "d49", ";", "r19"});
		
		/* Estado 48*/
		TAcc.addValor(48, new String[] {"let", "r20", "if", "r20",
									   "alert", "r20", "input", "r20",
									   "id", "r20", "return", "r20",
									   "function", "r20", "$", "r20",
									   "}", "r20"});
		
		/* Estado 49*/
		TAcc.addValor(49, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E1", "67", "E2", "28",
										"E3", "30", "U", "32"});

		/* Estado 50*/
		TAcc.addValor(50, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E2", "68", "E3", "30", "U", "32"});
		
		/* Estado 51*/
		TAcc.addValor(51, new String[] {"+", "r29", ")", "r29",
										";", "r29", ">", "r29",
										",", "r29"});
		
		/* Estado 52*/
		TAcc.addValor(52, new String[] {")", "d69", ">", "d49"});
		
		/* Estado 53*/
		TAcc.addValor(53, new String[] {"(", "d71", "+", "r33",
										")", "r33", ";", "r33",
										">", "r33", ",", "r33",
										"Q", "70"});
		
		/* Estado 54*/
		TAcc.addValor(54, new String[] {"let", "r43", "if", "r43",
										"alert", "r43", "input", "r43",
										"id", "r43", "function", "r43",
										"return", "r43", "$", "r43"});
		
		/* Estado 55*/
		TAcc.addValor(55, new String[] {"let", "r13", "if", "r13",
										"alert", "r13", "input", "r13",
										"id", "r13", "return", "r13",
										"}", "r13", "C", "72"});
		
		/* Estado 56*/
		TAcc.addValor(56, new String[] {")", "d73"});
		
		/* Estado 57*/
		TAcc.addValor(57, new String[] {"id", "d74"});
		
		/* Estado 58*/
		TAcc.addValor(58, new String[] {"(", "r44"});
		
		
		/* Estado 59*/
		TAcc.addValor(59, new String[] {";", "d75"});
		
		/* Estado 60*/
		TAcc.addValor(60, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E", "76", "E1", "27", "E2", "28",
										"E3", "30", "U", "32"});
		
		/* Estado 61*/
		TAcc.addValor(61, new String[] {"{", "d77"});
		
		/* Estado 62*/
		TAcc.addValor(62, new String[] {";", "d78"});
		
		/* Estado 63*/
		TAcc.addValor(63, new String[] {";", "d79"});
		
		/* Estado 64*/
		TAcc.addValor(64, new String[] {";", "r18"});
		
		/* Estado 65*/
		TAcc.addValor(65, new String[] {")", "r37"});
		
		/* Estado 66*/
		TAcc.addValor(66, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										"E", "80", "E1", "27", "E2", "28",
										"E3", "30", "U", "32"});
		
		/* Estado 67*/
		TAcc.addValor(67, new String[] {")", "r25", ";", "r25",
										">", "r25", ",", "r25",
										"+", "d50"});
		
		/* Estado 68*/
		TAcc.addValor(68, new String[] {")", "r27", ";", "r27",
										">", "r27", ",", "r27",
										"+", "r27"});
		
		/* Estado 69*/
		TAcc.addValor(69, new String[] {")", "r31", ";", "r31",
										">", "r31", ",", "r31",
										"+", "r31"});
		
		/* Estado 70*/
		TAcc.addValor(70, new String[] {")", "r32", ";", "r32",
										">", "r32", ",", "r32",
										"+", "r32"});
		
		/* Estado 71*/
		TAcc.addValor(71, new String[] {"!", "d29", "(", "d31",
										"ent", "d33", "cad", "d34",
										"++", "d35", "id", "r42",
										")", "r38",
										"L", "81", "E", "46", "E1", "27",
										"E2", "28", "E3", "30", "U", "32"});
		
		/* Estado 72*/
		TAcc.addValor(72, new String[] {"let", "d4", "if", "d5",
										"alert", "d7", "input", "d8",
										"id", "d9", "return", "d10",
										"}", "d82",
										"B", "83", "S", "6"});
		
		/* Estado 73*/
		TAcc.addValor(73, new String[] {"{", "r45"});
		
		/* Estado 74*/
		TAcc.addValor(74, new String[] {",", "d85", ")", "r52",
										"P1", "84"});
		
		/* Estado 75*/
		TAcc.addValor(75, new String[] {"let", "r5", "if", "r5", 
										"alert", "r5", "input", "r5",
										"id", "r5", "function", "r5",
										"return", "r5", "$", "r5",
										"}", "r5"});
		
		/* Estado 76*/
		TAcc.addValor(76, new String[] {";", "r10"});
		
		/* Estado 77*/
		TAcc.addValor(77, new String[] {"let", "r13", "if", "r13", 
										"alert", "r13", "input", "r13",
										"id", "r13", "return", "r13",
										"}", "r13", "C", "86"});
		
		/* Estado 78*/
		TAcc.addValor(78, new String[] {"let", "r21", "if", "r21", 
										"alert", "r21", "input", "r21",
										"id", "r21", "function", "r21",
										"return", "r21", "$", "r21",
										"}", "r21"});
		
		/* Estado 79*/
		TAcc.addValor(79, new String[] {"let", "r22", "if", "r22", 
										"alert", "r22", "input", "r22",
										"id", "r22", "function", "r22",
										"return", "r22", "$", "r22",
										"}", "r22"});
		
		/* Estado 80*/
		TAcc.addValor(80, new String[] {">", "d49", ",", "d66", 
										")", "r40",
										"L1", "87"});
		
		/* Estado 81*/
		TAcc.addValor(81, new String[] {")", "d88"});
		
		/* Estado 82*/
		TAcc.addValor(82, new String[] {"let", "r46", "if", "r46", 
										"alert", "r46", "input", "r46",
										"id", "r46", "function", "r46",
										"return", "r46", "$", "r46"});
		
		/* Estado 83*/
		TAcc.addValor(83, new String[] {"let", "r12", "if", "r12", 
										"alert", "r12", "input", "r12",
										"id", "r12", "return", "r12",
										"}", "r12"});
		
		/* Estado 84*/
		TAcc.addValor(84, new String[] {")", "r49"});
		
		/* Estado 85*/
		TAcc.addValor(85, new String[] {"number", "d16", "string", "d17",
										"boolean", "d18",
										"T", "89"});
		
		/* Estado 86*/
		TAcc.addValor(86, new String[] {"let", "d4", "if", "d5", 
										"alert", "d7", "input", "d8",
										"id", "d9", "return", "d10",
										"}", "d90",
										"B", "83", "S", "6"});
		
		/* Estado 87*/
		TAcc.addValor(87, new String[] {")", "r39"});
		
		/* Estado 88*/
		TAcc.addValor(88, new String[] {"+", "r34", ")", "r34",
										";", "r34", ">", "r34",
										",", "r34"});
		
		/* Estado 89*/
		TAcc.addValor(89, new String[] {"id", "d91"});
		
		/* Estado 90*/
		TAcc.addValor(90, new String[] {"else", "d93", "let", "r8",
										"if", "r8", "alert", "r8",
										"input", "r8", "id", "r8",
										"function", "r8", "return", "r8",
										"$", "r8", "}", "r8",
										"W", "92"});
		
		/* Estado 91*/
		TAcc.addValor(91, new String[] {",", "d85", ")", "r52",
										"P1", "94"});
		
		/* Estado 92*/
		TAcc.addValor(92, new String[] {"let", "r6", "if", "r6",
										"alert", "r6", "input", "r6",
										"id", "r6", "function", "r6",
										"return", "r6", "$", "r6",
										"}", "r6"});
		
		/* Estado 93*/
		TAcc.addValor(93, new String[] {"{", "d95"});
		
		/* Estado 94*/
		TAcc.addValor(94, new String[] {")", "r51"});
		
		/* Estado 95*/
		TAcc.addValor(95, new String[] {"let", "r13", "if", "r13",
										"alert", "r13", "input", "r13",
										"id", "r13", "return", "r13",
										"}", "r13",
										"C", "96"});
		
		/* Estado 96*/
		TAcc.addValor(96, new String[] {"}", "d97", "let", "d4",
										"if", "d5", "alert", "d7",
										"input", "d8", "id", "d9",
										"return", "d10",
										"B", "83", "S", "6"});
		
		/* Estado 97*/
		TAcc.addValor(97, new String[] {"let", "r7", "if", "r7",
										"alert", "r7", "input", "r7",
										"id", "r7", "function", "r7",
										"return", "r7", "$", "r7",
										"}", "r7"});	
	}
	
	
	/*
	 * Método que escribe recibe un cuarteto y lo escribe 
	 * en el fichero de Código Intermedio y se lo envía
	 * al Generador de Código Objeto.
	 */
	private static void emite (String op, Pair<String,String> arg1,
								Pair<String,String> arg2, Pair<String,String> res) {
				
		// Se pasa el cuarteto al Generador de Código Objeto
		GCO(op, arg1, arg2, res);
	}
	
	
	/*
	 * Función que devuelve la dirección del 
	 * identificador pasado como argumento.
	 */
	private static String buscarLugarTS(String id) {
		// Se busca el id en las TS
		if(!TSactiva) { // TSL activa
			if(TSL.containsKey(id))
				return (String) TSL.get(id).get(2);
			else
				return (String) TSG.get(id).get(2);
		}
		else { // TSG activa
			return (String) TSG.get(id).get(2);
		}
	}
	
	
	
	/*
	 * Función que devuelve la etiqueta asociada
	 * al identificador pasado como argumento.
	 */
	private static String buscarEtiqTS(String id) {
		// Se busca el id en las TS
		if(!TSactiva) { // TSL activa
			if(TSL.containsKey(id))
				return (String) TSL.get(id).get(6);
			else
				return (String) TSG.get(id).get(6);
		}
		else { // TSG activa
			return (String) TSG.get(id).get(6);
		}
	}
	
	
	/*
	 * Función que devuelve una nueva dirección temporal.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ArrayList<String> nuevaTemp (ArrayList atribs) {
		
		String lexema = "_temp"+nTemp++;
		
		String tipo = (String) atribs.get(1);
		String ancho = "";
		if(tipo.equals("ent"))
			ancho = "1";
		else if(tipo == "cad")
			ancho = "64";
		else
			ancho = "1";

		ArrayList arr = new ArrayList();
		arr.add(0, lexema);
		arr.add(1, tipo);
		arr.add(2, "");
		arr.add(3, atribs.get(3));
		if(atribs.get(4) instanceof ArrayList)
			arr.add(4, atribs.get(4));
		else
			arr.add(4, new ArrayList<String>());
		arr.add(5, atribs.get(5));
		arr.add(6, atribs.get(6));
	

		if(TSactiva) { // TSG activa
			arr.set(2, String.valueOf(despG)); //despG
			TSG.put(lexema, arr);
			despG = despG + Integer.valueOf(ancho);
		}
		else { // TSL activa
			arr.set(2, String.valueOf(despL)); //despL
			TSL.put(lexema, arr);
			despL = despL + Integer.valueOf(ancho);
		}
		
		ArrayList<String> res =  new ArrayList<String>();
		res.add(lexema);
		res.add((String) arr.get(2));
		
		return res;
	}
	
	
	/*
	 * Función que devuelve el tipo de la variable (global o local)
	 * con identificador <id>.
	 */
	private static String buscarClaseVariable(String id) {
		if(TSactiva)
			return "varGlobal";
		else {
			if(TSL.containsKey(id))
				return "varLocal";
			else
				return "varGlobal";
		}
	}
	
	
	
	/*
	 * Función que devuelve la dirección relativa de un operando.
	 * En caso de que una variable tenga desplazamiento > 127, devuelve
	 * la dirección y las instrucciones para llegar a esa dirección.
	 */
	private static ArrayList<String> dirOperando(Pair<String,String> op, String reg) {
		String tipoOp = op.getLeft();
		ArrayList<String> res = new ArrayList<String>();
		res.add(""); res.add("");
								
		// Variables globales
		if(tipoOp.equals("varGlobal")) {
			int desp = Integer.valueOf(op.getRight());
			String instr = "";
			if(desp > 127) {
				res.set(0, "[."+reg+"]");
				instr += "\t\tADD #127, .IY\n";
				desp -= 127;
				while(desp > 127) {
					instr += "\t\tADD #127, .A\n";
					desp -= 127;
				}
				instr += "\t\tADD #"+desp+", .A\n" +
						 "\t\tMOVE .A, ."+reg+"\n";
				res.set(1, instr);
			}
			else
				res.set(0, "#"+desp+"[.IY]");
		}
		
		// Variables locales, parámetros y variables temporales
		else if(tipoOp.equals("varLocal") || tipoOp.equals("param") 
				|| tipoOp.equals("varTemp")) {
			int desp = Integer.valueOf(op.getRight());
			String instr = "";
			if(desp > 127) {
				res.set(0, "[."+reg+"]");
				instr += "\t\tADD #127, .IX\n";
				desp -= 127;
				while(desp > 127) {
					instr += "\t\tADD #127, .A\n";
					desp -= 127;
				}
				instr += "\t\tADD #"+desp+", .A\n" +
						 "\t\tMOVE .A, ."+reg+"\n";
				res.set(1, instr);
			}
			else
				res.set(0, "#"+(1+desp)+"[.IX]");
		}
		
		// Constantes enteras
		else if (tipoOp.equals("cte_ent")) {
			res.set(0, "#"+op.getRight());
		}
		
		// Constantes cadenas
		else if (tipoOp.equals("cte_cad")) {
			try {
				FtempDatosCO.write("cad_"+nCadenas+": DATA \""+op.getRight()+"\"\n");
			} catch (IOException e) { e.printStackTrace(); }
							
			res.set(0, "#cad_"+nCadenas);
			
			nCadenas++;
		}
		
		// Etiquetas
		else if (tipoOp.equals("etiq")) {
			String etiq = op.getRight();
			if(etiq.startsWith("sig_instr")) {
				if(etiq.charAt(etiq.length()-1) == '1')
					res.set(0, "$3");
				else
					res.set(0, "$6");
			}
			else
				res.set(0, "/"+etiq);
		}
		
		return res;
	}
	
	
	
	/*
	 * Función que devuelve el tamaño de la zona de datos.
	 */
	private static String getSegDatosSize() {
		
		return String.valueOf(despG);
	}
	
	
	
	/*
	 * Método que escribe el tamaño del RA del procedimiento 
	 * pasado como argumento en el fichero de CO.
	 */
	private static void putTamRA(String nombreFun) {
		
		int tamVD = 0;
		int tamRA;
		
		if(nombreFun.equals("main")) {
			tamRA = 1+despG+tamVD;
		}
		else {
			String tipoVD = (String) TSG.get(nombreFun).get(5);
			if(tipoVD.equals("cad"))
				tamVD = 64;
			else if(tipoVD.equals("ent") || tipoVD.equals("log"))
				tamVD = 1; 
			else
				tamVD = 0;
			tamRA = 1+despL+tamVD;
		}

		try {
			FtempDatosCO.write("tam_RA_"+nombreFun+": EQU "+tamRA+"\n");
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	
	
	
	
	/*
	 * Función que devuelve el nombre de la función
	 * con etiqueta <etiq>.
	 */
	@SuppressWarnings("rawtypes")
	private static String buscarNombreFun(String etiq) {
		
		Iterator<String> it = TSG.keys().iterator();
		ArrayList atribs;
		String name = "";
		boolean found = false;
		while(!found && it.hasNext()) {
			atribs = TSG.get(it.next());
			if(atribs.get(1).equals("fun")
				&& atribs.get(6).equals(etiq)) {
				name = (String) atribs.get(0);
				found = true;
			}
		}
		if(!found)
			name = "main";
		return name;
	}
	
	
	
	
	/*
	 * Método que escribe las instrucciones 3-d en 
	 * el fichero de Código Intermedio.
	 */
	private static void writeToCIFile(String line) {
				
		// Se imprime el cuarteto en el fichero de Código Intermedio
		try {
			FCI.write(line+"\n");
		} catch (IOException e) { e.printStackTrace(); }
		
	}
	
	
	/*
	 * Método que escribe las instrucciones ensamblador
	 * el fichero de Código Objeto.
	 */
	private static void writeToCOFile(String lines) {
				
		try {
			FCO.write(lines);
		} catch (IOException e) { e.printStackTrace(); }
		
	}
	
	
	
	/*
	 * Método que escribe las instrucciones ensamblador
	 * del programa princial en un fichero temporal.
	 */
	private static void writeToTempCOFile(String lines) {
		
		try {
			FtempMainCO.write(lines);
		} catch (IOException e) { e.printStackTrace(); }
		
	}


	/*
	 * Método que escribe el comienzo del  
	 * fichero de Código Objeto.
	 */
	private static void writeHeaderToCOFile() {
		
		String header = 
			";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n" +
			";\t\t\t\t\t\t\t\t\t\t;\n" +
			";\t\tCompilado por CompiladorJSPDL\t;\n" +
			";\t\tVersión "+version+"\t\t\t\t\t\t;\n" +
			";\t\tErik Trujillo Guzmán\t\t\t;\n" +
			";\t\t\t\t\t\t\t\t\t\t;\n" +
			";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n\n" +
			"MOVE #inicio_datos_estaticos, .IY\n" +
			"MOVE #inicio_pila, .IX\n" + 
			"BR /main \t\t\t;salto al programa principal\n" +
			"\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n\n" +
			"\n;;;;;;;;;;;;;;;;;;;;; FUNCIONES ;;;;;;;;;;;;;;;;;;;;;;\n\n";
		
		try {
			FCO.write(header, 0, header.length());
		} catch (IOException e) { e.printStackTrace(); }
		
	}	
	
	
	/*
	 * Método que escribe el final del  
	 * fichero de Código Objeto.
	 */
	private static void writeAllToCOFile() {
		
		try {
			
			BufferedReader brTempMainCO = new BufferedReader(new FileReader("_tempMainCO.txt"));
			BufferedReader brTempDatosCO = new BufferedReader(new FileReader("_tempDatosCO.txt"));

			// Se escribe el programa principal en el fichero de Código Objeto
		    String line = brTempMainCO.readLine();
		    while(line != null) {
				FCO.write(line+"\n");
		        line = brTempMainCO.readLine();
		    }
		    
			// Se escriben los datos en el fichero de Código Objeto
		    line = brTempDatosCO.readLine();
		    while(line != null) {
				FCO.write(line+"\n");
		        line = brTempDatosCO.readLine();
		    }
		    
		    // Se cierran los ficheros abiertos
		    brTempMainCO.close();
		    brTempDatosCO.close();
		} catch (Exception e) { e.printStackTrace(); }
		
	}	
	
	

	
	
	
	
	/****************************************************************/
	
	@SuppressWarnings("rawtypes")
	public static void main (String[] args) {
		
		// Falta fichero fuente
		if(args.length == 0) {
			System.err.print("Error. Uso: java -jar "+nameFile+" fich_fuente.txt");
			return;
		}
		
		// Ayuda
		else if(args[0].equals("h") || args[0].equals("help")) {
			System.err.print("Uso: java -jar "+nameFile+" fich_fuente.txt");
			return;
		}
		
		// Información sobre el procesador
		else if(args[0].equals("i") || args[0].equals("info")) {
			System.err.print(nameFile+ "\n" +
							"Implementación de un Compilador para JavaScript-PDL.\n\n" +
							"Autor: "+author+".\n" +
							"Versión "+version+"\n" +
							"Creado en "+since+".\n" +
							"Última modificación en "+lastMod+ ".");
			return;
		}
		
		String fich = args[0];
		
		// Se llenan las tablas del Procesador
		llenadoTablas();
	
		// Se abre el fichero fuente
		try {
			bf = new BufferedReader(new FileReader(fich));
		} catch(FileNotFoundException e) { 
			System.err.print("Error: Fichero fuente no encontrado.");
			return; }
				
		try {
			// Se crea el fichero de tokens
			FTokens = new FileWriter(new File("tokens.txt"));
			
			// Se crea el fichero de Tabla de Simbolos
			FTS = new FileWriter(new File("TS.txt"));
			
			// Se crea el fichero de Errores
			FErr = new FileWriter(new File("errores.txt"));
			FErr.write("ERRORES DEL FICHERO FUENTE: \n\n");

			// Se crea el fichero parse
			FParse = new FileWriter(new File("parse.txt"));
			FParse.write("Ascendente ");
			
			// Se crea el fichero de Código Intermedio
			FCI = new FileWriter(new File("CI.txt"));
			
			// Se crea el fichero de Código Objeto
			FCO = new FileWriter(new File("CO.ens"));
			
			// Se crea el fichero temporal para el programa principal del Código Objeto
			FtempMainCO = new FileWriter(new File("_tempMainCO.txt"));
			
			// Se crea el fichero temporal para los datos del Código Objeto
			FtempDatosCO = new FileWriter(new File("_tempDatosCO.txt"));
			
			// Se escriben las primeras líneas del CO
			writeHeaderToCOFile();
			
			// Se escriben las primeras líneas de los ficheros temporales del GCO
			FtempMainCO.write("\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n\n" +
					 			"\n;;;;;;;;;;;;;;;;; PROGRAMA PRINCIPAL ;;;;;;;;;;;;;;;;;;;;\n\n" +
								"main:\tNOP\n");
			FtempDatosCO.write("\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n\n");
						
			// Se lee el primer carácter
			car = (char) bf.read();
		} catch (IOException e) { e.printStackTrace(); }

		System.out.print("Compilando programa fuente...");
		
		// Se guarda el momento del comienzo de la compilación
		long timeStart = System.currentTimeMillis();
		
		// Se llama al Analizador Sintáctico
		AnSt();
				
		// Se guarda el contenido de TSG en 
		// el buffer de TS
		bTS.add(0, new Pair<String, Map<String, ArrayList>>("",
					new HashTableMap<String, ArrayList>(TSG)));
		
		// Se borra el contenido de TSG
		Iterator<String> it = TSG.keys().iterator();
		while(it.hasNext())
			TSG.remove(it.next());
		
		// Se escribe el contenido de las TS
		// en el fichero TS.txt
		writeToTS();
		
		// Se escribe el tamaño del RA del programa principal
		putTamRA("main");
		
		
		// Se escriben las últimas líneas de los ficheros temporales de CO
		try {
			FtempMainCO.write("\n\t\tHALT\n\n");
			FtempDatosCO.write( "\ninicio_datos_estaticos: RES "+getSegDatosSize()+"\n" +
								"inicio_pila: NOP\n" +
								"\nEND");
		} catch (IOException e) { e.printStackTrace(); }
				
		// Se cierran todos los ficheros abiertos
		try {			
		    bf.close();
			FTokens.close();
		    FTS.close();	
			FErr.close();	
			FParse.close();	
			FCI.close();
			FtempMainCO.close();
			FtempDatosCO.close();
			
			// Se escriben las últimas líneas del fichero de CO
			writeAllToCOFile();
			FCO.close();

		} catch (IOException e) { e.printStackTrace(); }	
		
		
		// Se borran los ficheros temporales
		File FtempMainCO = new File("_tempMainCO.txt");
		FtempMainCO.delete();
		File FtempDatosCO = new File("_tempDatosCO.txt");
		FtempDatosCO.delete();

          		
		// Mensaje final del Compilador
		// Programa fuente correcto
		if(pilaAtrib.get(pilaAtrib.size()-2).get(0).equals(("A")) &&
			pilaAtrib.get(pilaAtrib.size()-2).get(1).equals(("tipo_ok"))) {
			
			// Se guarda el momento del fin de la compilación
			long timeEnd = System.currentTimeMillis();
			
			System.out.println(" Hecho!");
			try {
				Thread.sleep((long) 500);
			} catch (InterruptedException e) { e.printStackTrace(); }	

			System.out.print("Progama fuente compilado en "+(timeEnd - timeStart)+" ms.");
		}
		
		// Programa fuente con errores
		else {
			System.out.println("");
			try {
				Thread.sleep((long) 500);
			} catch (InterruptedException e) { e.printStackTrace(); }	

			System.out.print("El programa fuente contiene errores... " + 
					"Consulte el fichero \"errores.txt\" para más información.");
		}
	}
}
