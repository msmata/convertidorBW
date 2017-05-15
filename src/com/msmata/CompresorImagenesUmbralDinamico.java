package com.msmata;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.plaf.BorderUIResource;

import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFT6Compressor;

public class CompresorImagenesUmbralDinamico {
	private static final double MARGEN_DERECHO_UMBRAL = 0.70;
	private static final double MARGEN_SUP_UMBRAL = 0.15;//El porcentaje superior de la imagen al cual se aplica el umbral superior
//	private static final int UMBRAL_SUPERIOR = 146;
//	private static final int UMBRAL = 90;
	private static final double RESIZE_PERCENTAGE = 0.70;

	public static void main(String[] args) {

		String filename = "Pampa 5699.tif";
		String path = "C:\\cafi\\NUEVOS\\Grises\\";
		
		comprimirImagenEscalaGrises(filename, path);
	}

	public static void comprimirImagenEscalaGrises(String filename, String path) {
		File input = new File(path + filename);
		BufferedImage image;
		try {
//			long time = System.currentTimeMillis();
//			long totalTime = time;
			image = ImageIO.read(input);
//			System.out.println("Lectura de imagen " + input.getName() + " : " + (System.currentTimeMillis() - time) + " milisegundos");

			// Convertir los pixeles a blanco o negro
//			time = System.currentTimeMillis();
			
			int umbralElegido = elegirUmbralConMejorVarianza(image);
			BufferedImage finalThresholdImage = aplicarUmbral(image, umbralElegido);
			
			// Achicar la imagen un determinado porcentaje
//			time = System.currentTimeMillis();
			BufferedImage resizedImage = resize(finalThresholdImage, RESIZE_PERCENTAGE);
//			System.out.println("Resized de imagen: " + (System.currentTimeMillis() - time) + " milisegundos");


			
//			aplicarRecuadroNegro(image, UMBRAL);
//			System.out.println("Aplicar umbral: " + (System.currentTimeMillis() - time) + " milisegundos");

			// Pasar de modo grayscale a modo indexado 1 bit
//			time = System.currentTimeMillis();
//			BufferedImage singleBitImage = ConvertUtil.convert1(finalThresholdImage);
//			System.out.println("Pasar de modo grayscale a modo indexado 1 bit: " + (System.currentTimeMillis() - time) + " milisegundos");
			// Se comprime a CCITT T.4
//			time = System.currentTimeMillis();
			comprimirImagen(resizedImage, path + input.getName());
//			System.out.println("Compresion CCITT T.4: " + (System.currentTimeMillis() - time) + " milisegundos");
//			System.out.println("Proceso finalizado en " + (System.currentTimeMillis() - totalTime) + " milisegundos");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static BufferedImage aplicarUmbral(BufferedImage image, int umbral) {
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				if (c.getRed() < umbral) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		return finalThresholdImage;
	}

	private static void comprimirImagen(BufferedImage singleBitImage, String filename) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("tif").next();
		TIFFImageWriteParam writeParam = (TIFFImageWriteParam) writer.getDefaultWriteParam();
		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		writeParam.setCompressionType("CCITT T.6");
		writeParam.setTIFFCompressor(new TIFFT6Compressor());

		File outputFile = new File(filename + "_procesado_umenorVarianza.tif");
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);
		writer.setOutput(ios);
		// Pruebas 28-3
		writer.prepareWriteSequence(null);
		ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(singleBitImage);
		javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(spec, writeParam);
		IIOImage iioImage = new IIOImage(singleBitImage, null, metadata);
		writer.writeToSequence(iioImage, writeParam);
		singleBitImage.flush();
		writer.endWriteSequence();
		ios.flush();
		writer.dispose();
		ios.close();
		// writer.write(singleBitImage);
	}

//	private static BufferedImage aplicarUmbral(BufferedImage image, int umbral) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//		int pixelBlanco = mixColor(0, 0, 0);
//		int pixelNegro = mixColor(255, 255, 255);
//		
//		for (int i = 0; i < image.getWidth(); i++) {
//			for (int j = 0; j < image.getHeight(); j++) {
//				Color c = new Color(image.getRGB(i, j));
//				if (c.getRed() < umbral) {// Como son grises las 3 componentes son iguales
//					finalThresholdImage.setRGB(i, j, pixelBlanco);
//				} else {
//					finalThresholdImage.setRGB(i, j, pixelNegro);
//				}
//			}
//		}
//		return finalThresholdImage;
//	}
	
	private static BufferedImage aplicarUmbralPromedio(BufferedImage image) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
		int umbralPromedio = calcularPromedioGrises(image);
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				//Probar esto
				if (c.getRed() < umbralPromedio) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		
		return finalThresholdImage;
	}
	
	private static BufferedImage aplicarUmbralModa(BufferedImage image) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
		int umbralModa = calcularModaGrises(image);
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				//Probar esto
				if (c.getRed() < umbralModa) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		
		return finalThresholdImage;
	}
	
	private static BufferedImage aplicarUmbralPromedioRegional(BufferedImage image) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
//		int umbralModa = calcularModaGrises(image);
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				
				Color c = new Color(image.getRGB(i, j));
				//Probar esto
				if (c.getRed() < obtenerPromedioRegion(image, i, j)) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		
		return finalThresholdImage;
	}
	
	private static int obtenerPromedioRegion(BufferedImage image, int x, int y){
		
		int tamañoRegion = 10;
		
		int desdeX = ((x - tamañoRegion) < 0) ? 0 : x - tamañoRegion;
		int hastaX = ((x + tamañoRegion) > image.getWidth()) ? image.getWidth() : x + tamañoRegion;
		int desdeY = ((y - tamañoRegion) < 0) ? 0 : y - tamañoRegion;
		int hastaY = ((y + tamañoRegion) > image.getHeight()) ? image.getHeight() : y + tamañoRegion;
		
		long totalGrises = 0;
		long cantidadPixeles = (hastaX - desdeX) * (hastaY - desdeY);
		
		for (int i = desdeX;i < hastaX;i++){
			for (int j = desdeY;j < hastaY;j++){
				Color c = new Color(image.getRGB(i, j));
				totalGrises += c.getRed();
			}
		}
		
		return (int)(totalGrises / cantidadPixeles);
	}
	
	private static int calcularPromedioGrises(BufferedImage image){
		long totalGrises = 0;
		long cantidadPixeles = image.getWidth() * image.getHeight();
		
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				totalGrises  += c.getRed();
			}
		}
				
		return (int)(totalGrises / cantidadPixeles);
	}
	
	private static int calcularModaGrises(BufferedImage image){
		long[] histograma = new long[256];
		
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				histograma[c.getRed()]++;
			}
		}
		
		int nivelMasOcurrencias = -1;
		long cantidadOcurrenciasMax = 0;
		
		for (int i = 0;i < 256;i++){
			if (histograma[i] > cantidadOcurrenciasMax){
				cantidadOcurrenciasMax = histograma[i];
				nivelMasOcurrencias = i;
			}
		}
				
		return nivelMasOcurrencias;
	}
	
//	private static void aplicarRecuadroNegro(BufferedImage image, int umbral) {
//		int pixelNegro = mixColor(0, 0, 0);
//		
//		int bordeVertical = (int)(image.getWidth() * MARGEN_DERECHO_UMBRAL);
//		int bordeHorizontal = (int)(image.getHeight() * MARGEN_SUP_UMBRAL);
//			
//		//Dibujar raya vertical
//		for (int i = 0; i < bordeHorizontal; i++) {
//			image.setRGB(bordeVertical, i, pixelNegro);
//		}
//		
//		//Dibujar raya horizontal
//		for (int i = bordeVertical; i < image.getWidth(); i++) {
//			image.setRGB(i, bordeHorizontal, pixelNegro);
//		}
//		
//		File outputFile = new File("C:\\cafi\\NUEVOS\\Grises\\_bordeNegro.tif");
//
//		try {
//			ImageIO.write(image, "TIF", outputFile);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	private static BufferedImage resize(BufferedImage originalImage, double percent) {
		// creates output image
		int newWidth = (int) (originalImage.getWidth() * percent);
		int newHeight = (int) (originalImage.getHeight() * percent);
		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight,	originalImage.getType());

		// scales the input image to the output image
		Graphics2D g2d = resizedImage.createGraphics();
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		
		g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
		g2d.dispose();

		return resizedImage;
	}

	private static int mixColor(int red, int green, int blue) {
		return red << 16 | green << 8 | blue;
	}
	
	private static BufferedImage aplicarUmbralPromedioRegionesFijas(BufferedImage image) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
//		int umbralModa = calcularModaGrises(image);
		Map<Region, Integer> promedioRegiones = obtenerPromedioRegiones(image, 25);
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				
				Color c = new Color(image.getRGB(i, j));
				
				int umbral = 0;
				
				for (Map.Entry<CompresorImagenesUmbralDinamico.Region,Integer> entry : promedioRegiones.entrySet()) {
					Region region = entry.getKey();
					if (i >= region.getDesdeX() && i <= region.getHastaX() && j >= region.getDesdeY() && j <= region.getHastaY()){
						umbral = entry.getValue();
						break;
					}
				}
				
				//Probar esto
				if (c.getRed() < umbral) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		
		return finalThresholdImage;
	}
	
	private static int elegirUmbralConMejorVarianza(BufferedImage image){
		
		double minVarianza = 1000000;
		int umbral = 0;
		
		for (int i = 0;i < 256;i++) {
			double varianza = calcularVarianza(image, i);
			if (varianza < minVarianza){
				minVarianza = varianza;
				umbral = i;
			}
		}
		
		return umbral;
	}
	
	private static double calcularVarianza(BufferedImage image, int umbral){
	
		long sumatoriaCuadrado = 0;
		
		for (int i = 0;i < image.getWidth();i++){
			for (int j = 0;j < image.getHeight();j++){
				Color c = new Color(image.getRGB(i, j));
				sumatoriaCuadrado += Math.pow(c.getRed() - umbral, 2);
			}
		}
		
		return sumatoriaCuadrado / ((image.getWidth() * image.getHeight()) - 1);
	}
	
	private static BufferedImage aplicarUmbralPromedioFranjas(BufferedImage image) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
//		int umbralModa = calcularModaGrises(image);
		Map<Region, Integer> promedioRegiones = obtenerPromedioFranjasHorizontales(image, 30);
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				
				Color c = new Color(image.getRGB(i, j));
				
				int umbral = 0;
				
				for (Map.Entry<CompresorImagenesUmbralDinamico.Region,Integer> entry : promedioRegiones.entrySet()) {
					Region region = entry.getKey();
					if (i >= region.getDesdeX() && i <= region.getHastaX() && j >= region.getDesdeY() && j <= region.getHastaY()){
						umbral = entry.getValue();
						break;
					}
				}
				
				//Probar esto
				if (c.getRed() < umbral) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		
		return finalThresholdImage;
	}
	
	private static BufferedImage aplicarUmbralPromedioFranjasVerticales(BufferedImage image) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
//		int umbralModa = calcularModaGrises(image);
		Map<Region, Integer> promedioRegiones = obtenerPromedioFranjasVerticales(image, 20);
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				
				Color c = new Color(image.getRGB(i, j));
				
				int umbral = 0;
				
				for (Map.Entry<CompresorImagenesUmbralDinamico.Region,Integer> entry : promedioRegiones.entrySet()) {
					Region region = entry.getKey();
					if (i >= region.getDesdeX() && i <= region.getHastaX() && j >= region.getDesdeY() && j <= region.getHastaY()){
						umbral = entry.getValue();
						break;
					}
				}
				
				//Probar esto
				if (c.getRed() < umbral) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		
		return finalThresholdImage;
	}
	
	private static Map<Region, Integer> obtenerPromedioRegiones(BufferedImage image, int particiones){
		Map<Region, Integer> regiones = new HashMap<CompresorImagenesUmbralDinamico.Region, Integer>();
		
		int anchoParticion = image.getWidth() / particiones;
		int altoParticion = image.getHeight() / particiones;
		
		for (int i = 0;i < image.getWidth();i += anchoParticion){
			for (int j = 0;j < image.getHeight();j += altoParticion){
				Region region = new Region();
				region.setDesdeX(i);
				region.setDesdeY(j);
				region.setHastaX((i + anchoParticion) > image.getWidth() ? image.getWidth() : i + anchoParticion);
				region.setHastaY((j + altoParticion) > image.getHeight() ? image.getHeight() : j + altoParticion);
				
				long totalGrises = 0;
				long cantidadPixeles = (region.getHastaX() - region.getDesdeX()) * (region.getHastaY() - region.getDesdeY());
				
				for (int x = region.getDesdeX();x < region.getHastaX() && x < image.getWidth();x++){
					for (int y = region.getDesdeY();y < region.getHastaY() && y < image.getHeight();y++){
						Color c = new Color(image.getRGB(x, y));
						totalGrises += c.getRed();
					}
				}
				
				regiones.put(region, totalGrises != 0 ? (int)(totalGrises / cantidadPixeles) : 0);
			}
		}
		
		return regiones;
	}
	
	private static Map<Region, Integer> obtenerPromedioFranjasHorizontales(BufferedImage image, int franjas){
		Map<Region, Integer> regiones = new HashMap<CompresorImagenesUmbralDinamico.Region, Integer>();
		
		int altoParticion = image.getHeight() / franjas;
		
		for (int i = 0;i < image.getWidth();i++){
			for (int j = 0;j < image.getHeight();j += altoParticion){
				Region region = new Region();
				region.setDesdeX(i);
				region.setDesdeY(j);
				region.setHastaX(image.getWidth());
				region.setHastaY((j + altoParticion) > image.getHeight() ? image.getHeight() : j + altoParticion);
				
				long totalGrises = 0;
				long cantidadPixeles = (region.getHastaX() - region.getDesdeX()) * (region.getHastaY() - region.getDesdeY());
				
				for (int x = region.getDesdeX();x < region.getHastaX() && x < image.getWidth();x++){
					for (int y = region.getDesdeY();y < region.getHastaY() && y < image.getHeight();y++){
						Color c = new Color(image.getRGB(x, y));
						totalGrises += c.getRed();
					}
				}
				
				regiones.put(region, totalGrises != 0 ? (int)(totalGrises / cantidadPixeles) : 0);
			}
		}
		
		return regiones;
	}
	
	private static Map<Region, Integer> obtenerPromedioFranjasVerticales(BufferedImage image, int franjas){
		Map<Region, Integer> regiones = new HashMap<CompresorImagenesUmbralDinamico.Region, Integer>();
		
		int anchoParticion = image.getWidth() / franjas;
		
		for (int i = 0;i < image.getWidth();i += anchoParticion){
			for (int j = 0;j < image.getHeight();j++){
				Region region = new Region();
				region.setDesdeX(i);
				region.setDesdeY(j);
				region.setHastaX((i + anchoParticion) > image.getWidth() ? image.getWidth() : i + anchoParticion);
				region.setHastaY(image.getHeight());
				
				long totalGrises = 0;
				long cantidadPixeles = (region.getHastaX() - region.getDesdeX()) * (region.getHastaY() - region.getDesdeY());
				
				for (int x = region.getDesdeX();x < region.getHastaX() && x < image.getWidth();x++){
					for (int y = region.getDesdeY();y < region.getHastaY() && y < image.getHeight();y++){
						Color c = new Color(image.getRGB(x, y));
						totalGrises += c.getRed();
					}
				}
				
				regiones.put(region, totalGrises != 0 ? (int)(totalGrises / cantidadPixeles) : 0);
			}
		}
		
		return regiones;
	}
	
	private static class Region {
		private int desdeX;
		private int desdeY;
		private int hastaX;
		private int hastaY;
		
		public int getDesdeX() {
			return desdeX;
		}
		public void setDesdeX(int desdeX) {
			this.desdeX = desdeX;
		}
		public int getDesdeY() {
			return desdeY;
		}
		public void setDesdeY(int desdeY) {
			this.desdeY = desdeY;
		}
		public int getHastaX() {
			return hastaX;
		}
		public void setHastaX(int hastaX) {
			this.hastaX = hastaX;
		}
		public int getHastaY() {
			return hastaY;
		}
		public void setHastaY(int hastaY) {
			this.hastaY = hastaY;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Region))
				return false;
			
			Region r = (Region)obj;
			
			return desdeX == r.getDesdeX() && desdeY == r.getDesdeY() && hastaX == r.getHastaX() && hastaY == r.getHastaY();
		}
	}
	
}
