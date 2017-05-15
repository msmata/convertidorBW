package com.msmata;

import java.io.File;
import java.io.FilenameFilter;

public class CompresorMultiple {

	private static String DIRECTORIO_IMAGENES = "C:\\cafi\\NUEVOS\\Grises\\nuevasPruebas\\";
	
	public static void main(String[] args) {
		
		File directorioImagenes = new File(DIRECTORIO_IMAGENES);
		
		String[] imagenes = directorioImagenes.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".tif");
			}
		});
		
		long totalTime = System.currentTimeMillis();
		System.out.println("Iniciando conversión de " + imagenes.length + " imágenes");
		for (String imagenTif : imagenes) {
			CompresorImagenes.comprimirImagenEscalaGrises(imagenTif, DIRECTORIO_IMAGENES);
		}
		
		System.out.println("Tiempo total de proceso: " + (System.currentTimeMillis() - totalTime) + " milisegundos");
	}
}
