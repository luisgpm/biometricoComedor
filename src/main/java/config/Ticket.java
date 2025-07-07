/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package config;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.InputStream;
import java.sql.Timestamp;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;


/**
 *
 * @author lporcayo
 */
public class Ticket {
    
    public static boolean imprimirTicket(String nombre, String id, Timestamp fecha) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService selectedService = null;
        Config config = ConfigManager.loadConfig();
        
        if (config == null || config.printerName == null || config.printerName.isEmpty()) {
            Mensajes.mostrarMensajeAutoCierre("No hay impresora Configurada.", 2000);
            return false;
        }
        for (PrintService service : services) {
            if (service.getName().equals(config.printerName)) {
                selectedService = service;
                break;
            }
        }
        if (selectedService == null) {
            Mensajes.mostrarMensajeAutoCierre("No se encontró la impresora configurada: " + config.printerName, 2000);
            return false;
        }

        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(selectedService);
            PageFormat pageFormat = job.defaultPage();
            Paper paper = new Paper();
            
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
            pageFormat.setPaper(paper);
            
            job.setPrintable((graphics, pf, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

                Graphics2D g2d = (Graphics2D) graphics;
                g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
                g2d.translate(pf.getImageableX(), pf.getImageableY());

                try (InputStream imgStream = Ticket.class.getClassLoader().getResourceAsStream("apollo.png")) {
                    if (imgStream == null) {
                        throw new Exception("No se encontró el recurso apollo.png en el classpath.");
                    }
                    BufferedImage logo = ImageIO.read(imgStream);
                    g2d.drawImage(logo, 50, 0, 100, 50, null);
                } catch (Exception e) {
                    Mensajes.mostrarMensajeAutoCierre("❗ Error al cargar la imagen: " + e.getMessage(), 3000);
                }

                g2d.drawString("Nombre: " + nombre, 5, 70);
                g2d.drawString("NE: " + id, 5, 90);
                g2d.drawString("Fecha: " + fecha.toLocalDateTime().toLocalDate(), 5, 110);
                g2d.drawString("Hora: " + fecha.toLocalDateTime().toLocalTime(), 5, 130);
                g2d.drawString(" ", 5, 150);
                g2d.drawString(" ", 5, 170);

                


                return Printable.PAGE_EXISTS;
            }, pageFormat);
            job.print();
            // System.out.println("Impresión realizada correctamente.");
            Mensajes.mostrarMensajeAutoCierre("Impresión realizada correctamente.", 3000);
            return true;

        } catch (Exception e) {
            // System.out.println("Error al imprimir: " + e.getMessage());
            Mensajes.mostrarMensajeAutoCierre("❗ Error al imprimir: " + e.getMessage(), 3000);
            return false;
        }
    }


}
