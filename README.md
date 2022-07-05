<a name="top"></a>
# Proyecto Compilador de JavaScript-PDL
Proyecto de Compilador de JavaScript-PDL
(*Procesadores de Lenguajes 2020/2021 y Traductores de Lenguajes 2021/2022 ETSIINF - UPM*)


## Autor

- Erik Trujillo Guzmán


##  Índice

1. [Compilador](#apartado1)
2. [Archivos del repositorio](#apartado2)
3. [Cómo utilizar el Procesador](#apartado3)



<a name="apartado1"></a>
## 1. Compilador

Este proyecto desarrolla un Compilador de un subconjunto de JavaScript, en concreto, del subconjunto JavaScript-PDL. El Compilador recibe un fichero fuente escrito en JavaScript-PDL, comprueba que esté escrito correctamente y traduce el programa fuente al lenguaje ensamblador ENS2011 generando los siguientes ficheros:

- **errores.txt**: fichero que contiene los errores encontrados en el fichero fuente.

- **parse.txt**: fichero que contiene el parse, es decir, los números de las reglas de la gramática utilizadas por el Analizador Sintáctico.

- **tokens.txt**: fichero que contiene todos los tokens encontrados en el fichero fuente.

- **TS.txt**: fichero que contiene las Tablas de Símbolos utilizadas por el Procesador.

- **CI.txt**: fichero que contiene la traducción del lenguaje fuente a lenguaje intermedio (código de tres direcciones).

- **CO.txt**: fichero que contiene la traducción del lenguaje fuente a lenguaje ensamblador ENS2001.


Este proyecto está escrito en Java e implementa un Analizador Léxico, un Analizador Sintáctico Ascendente, un Analizador Semántico, un Generador de Código Intermedio, un Generador de Código Objeto y un Gestor de Errores, así como todas las herramientas que necesitan dichos analizadores como la Tabla de Símbolos o el Autómata Finito Determinista reconocedor de los prefijos viables de la gramatica.


<a name="apartado2"></a>
## 2. Archivos del repositorio


### doc/

Contiene la documentación del proyecto.

- **ENS2011.pdf**: archivo PDF con la documentación del lenguaje ensamblador ENS2001.

- **Mensajes de error.txt**: archivo de texto que contiene todos los mensajes de error que proporciona el Procesador.


### Compilador/

Contiene el código fuente del Compilador.

- **src/**: directorio que contiene los ficheros fuente de los que hace uso el Compilador.

- **pruebas/**: directorio que contiene casos de prueba, con los resultados de la ejecución de cada prueba.

- **fuente.txt**: fichero que contiene el programa fuente a analizar por el Compilador. Puede ser modificado.

- **CompiladorJSPDL.jar**: fichero ejecutable del Compilador.


<a name="vast"></a>
### ENS2001-Windows/

Este directorio contiene el simulador del lenguaje ensamblador ENS2001. Gracias a esta herramienta es posible ejecutar el código ensamblador generado por el Compilador. Contiene ejemplos para ver su uso.

Para utilizar el simluador ejecute el archivo "*ENS2001-Windows/winens.exe*". Esto hará que se abran varias ventanas con el simulador. Para ponerlo en funcionamiento debe seleccionar `Archivo -> Abrir y ensamblar...` y seleccionar el fichero de Código Objeto "*Compilador/CO.txt*", que se habrá generado al haber ejecutado el Compilador con un fichero fuente que no contenga errores. Una vez hecho esto, seleccione `Archivo -> Ejecutar` para comenzar la ejecución del programa.


[Back to top](#top)
	
	
<a name="apartado3"></a>
## 3. Cómo utilizar el Compilador


Para poder ejecutar el Compilador debe posicionarse dentro de la carpeta "*Compilador*" y teclear en una ventana MS-DOS de Windows:

	java -jar CompiladorJSPDL.java fuente.txt
	
El archivo "*fuente.txt*" es el programa fuente escrito en JavaScript-PDL que analizará el Compilador.

Para ver información sobre el Procesador ejecute:

	java -jar CompiladorJSPDL.java info


Para mostrar esta ayuda sobre el uso puede ejecutar: 
	
	java -jar CompiladorJSPDL.java h o java -jar CompiladorJSPDL.java help

	
[Back to top](#top)



**¡Gracias por usar mi Compilador! :smile:**


[Back to top](#top)
		
