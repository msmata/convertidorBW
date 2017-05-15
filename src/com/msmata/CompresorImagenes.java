package com.msmata;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.xmlgraphics.image.writer.ImageWriterParams;
import org.apache.xmlgraphics.image.writer.ImageWriterRegistry;
import org.apache.xmlgraphics.image.writer.MultiImageWriter;
//import javax.imageio.ImageWriteParam;

public class CompresorImagenes {
	private static final double MARGEN_DERECHO_UMBRAL = 0.70;
	private static final double MARGEN_SUP_UMBRAL = 0.15;//El porcentaje superior de la imagen al cual se aplica el umbral superior
	private static final int UMBRAL_SUPERIOR = 146;
	private static final int UMBRAL = 90;
	private static final double RESIZE_PERCENTAGE = 0.90;

	public static void main(String[] args) {

		String filename = "Nacion9292.tif";
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
			BufferedImage finalThresholdImage = aplicarUmbralSelectivo(image, UMBRAL);
			
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
			comprimirImagenApache(resizedImage, path + input.getName());
//			System.out.println("Compresion CCITT T.4: " + (System.currentTimeMillis() - time) + " milisegundos");
//			System.out.println("Proceso finalizado en " + (System.currentTimeMillis() - totalTime) + " milisegundos");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//BUG Imagen con pixeles invertidos en W2000
//	private static void comprimirImagen(BufferedImage singleBitImage, String filename) throws IOException {
//		ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff").next();
//		TIFFImageWriteParam writeParam = (TIFFImageWriteParam) writer.getDefaultWriteParam();
//		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//		writeParam.setCompressionType("CCITT T.6");
//		writeParam.setTIFFCompressor(new TIFFT6Compressor());
//
//		File outputFile = new File(filename + "_procesado_T6tiff.tif");
//		ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);
//		writer.setOutput(ios);
//		// Pruebas 28-3
//		writer.prepareWriteSequence(null);
//		ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(singleBitImage);
//		javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(spec, writeParam);
//		IIOImage iioImage = new IIOImage(singleBitImage, null, metadata);
//		writer.writeToSequence(iioImage, writeParam);
//		singleBitImage.flush();
//		writer.endWriteSequence();
//		ios.flush();
//		writer.dispose();
//		ios.close();
//		// writer.write(singleBitImage);
//	}
	
	private static void comprimirImagenApache(BufferedImage singleBitImage, String filename) throws IOException {

		OutputStream out = new FileOutputStream(filename + "_procesado_T6apacheconfecha.tif");
        out = new BufferedOutputStream(out);
		org.apache.xmlgraphics.image.writer.ImageWriter writer = ImageWriterRegistry.getInstance().getWriterFor("image/tiff");
		ImageWriterParams params = new ImageWriterParams();
        params.setCompressionMethod("CCITT T.6");
        params.setResolution(90);
        MultiImageWriter multiWriter = writer.createMultiImageWriter(out);
        multiWriter.writeImage(singleBitImage, params);
        multiWriter.close();
        out.close();
	}
	
//	private static void comprimirImagenSinMetadata(BufferedImage singleBitImage, String filename) throws IOException {
//		ImageWriter writer = ImageIO.getImageWritersByFormatName("tif").next();
//		TIFFImageWriteParam writeParam = (TIFFImageWriteParam) writer.getDefaultWriteParam();
//		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//		writeParam.setCompressionType("LZW");
//		writeParam.setTIFFCompressor(new TIFFLZWCompressor(1));
//
//		File outputFile = new File(filename + "_procesado_90.tif");
//		ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);
//		writer.setOutput(ios);
//		// Pruebas 28-3
////		writer.prepareWriteSequence(null);
//		ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(singleBitImage);
//		javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(spec, writeParam);
//		IIOImage iioImage = new IIOImage(singleBitImage, null, null);
////		writer.writeToSequence(iioImage, writeParam);
//		writer.write(metadata, iioImage, writeParam);
//		singleBitImage.flush();
////		writer.endWriteSequence();
//		ios.flush();
//		writer.dispose();
//		ios.close();
//		// writer.write(singleBitImage);
//	}

	private static BufferedImage aplicarUmbral(BufferedImage image, int umbral) {
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
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
	
	private static BufferedImage aplicarUmbralSelectivo(BufferedImage image, int umbral) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
//		Caja cajaImporte = new Caja(((int)(image.getWidth() * MARGEN_DERECHO_UMBRAL)), 0, image.getWidth(), ((int)(image.getHeight() * MARGEN_SUP_UMBRAL)));
//		Caja cajaFecha = new Caja(((int)(image.getWidth() * 0.17)), ((int)(image.getHeight() * 0.15)), 
//								  ((int)(image.getWidth() * 0.70)), ((int)(image.getHeight() * 0.55)));
		
		Cajas cajas = new Cajas(image.getWidth(), image.getHeight());
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				//Probar esto
//				int umbralUtilizado = j < image.getHeight() * MARGEN_SUP_UMBRAL ? UMBRAL_SUPERIOR : umbral;
//				int umbralUtilizado = cajaImporte.pertenece(i, j) || cajaFecha.pertenece(i, j) ? UMBRAL_SUPERIOR : umbral;
				int umbralUtilizado = cajas.pertenece(i, j) ? UMBRAL_SUPERIOR : umbral;
				if (c.getRed() < umbralUtilizado) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		return finalThresholdImage;
	}
	
	private static void aplicarRecuadroNegro(BufferedImage image, int umbral) {
		int pixelNegro = mixColor(0, 0, 0);
		
		int bordeVertical = (int)(image.getWidth() * MARGEN_DERECHO_UMBRAL);
		int bordeHorizontal = (int)(image.getHeight() * MARGEN_SUP_UMBRAL);
			
		//Dibujar raya vertical
		for (int i = 0; i < bordeHorizontal; i++) {
			image.setRGB(bordeVertical, i, pixelNegro);
		}
		
		//Dibujar raya horizontal
		for (int i = bordeVertical; i < image.getWidth(); i++) {
			image.setRGB(i, bordeHorizontal, pixelNegro);
		}
		
		File outputFile = new File("C:\\cafi\\NUEVOS\\Grises\\_bordeNegro.tif");

		try {
			ImageIO.write(image, "TIF", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
	
	private static class Caja {
		private Integer x1;
		private Integer y1;
		private Integer x2;
		private Integer y2;
		
		public Caja(Integer x1, Integer y1, Integer x2, Integer y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}

		public boolean pertenece(Integer x, Integer y){
			return (x <= x2 && x >= x1 && y <= y2 && y >= y1);
		}
	}
	
	private static class Cajas extends ArrayList<Caja>{
		
		public Cajas(int width, int height) {
			Caja cajaImporte = new Caja(((int)(width * MARGEN_DERECHO_UMBRAL)), 0, 
										       width, ((int)(height * MARGEN_SUP_UMBRAL)));
			Caja cajaFecha = new Caja(((int)(width * 0.17)), ((int)(height * 0.15)), 
									  ((int)(width * 0.70)), ((int)(height * 0.55)));
			this.add(cajaFecha);
			this.add(cajaImporte);
		}
		
		public boolean pertenece(Integer x, Integer y){
			boolean pertenece = false;
			
			for (Caja caja : this) {
				if (caja.pertenece(x, y))
					return true;
			}
			
			return pertenece;
		}
	}
}
