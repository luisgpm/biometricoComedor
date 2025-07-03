/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package config;

import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;

/**
 *
 * @author lporcayo
 */
public class PrinterSelector {
    public static String selectPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        String[] printerNames = new String[services.length];
        for (int i = 0; i < services.length; i++) {
            printerNames[i] = services[i].getName();
        }
        String selected = (String) JOptionPane.showInputDialog(
                null, "Selecciona la impresora:", "Impresoras",
                JOptionPane.QUESTION_MESSAGE, null, printerNames, printerNames.length > 0 ? printerNames[0] : null
        );
        return selected;
    }

    public static boolean testPrint(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            if (service.getName().equals(printerName)) {
                try {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPrintService(service);
                    job.setPrintable((graphics, pf, pageIndex) -> {
                        if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                        graphics.drawString("¡Prueba de impresión exitosa!", 100, 100);
                        return Printable.PAGE_EXISTS;
                    });
                    job.print();
                    JOptionPane.showMessageDialog(null, "Prueba de impresión enviada.");
                    return true;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error al imprimir: " + e.getMessage());
                    return false;
                }
            }
        }
        JOptionPane.showMessageDialog(null, "No se encontró la impresora seleccionada.");
        return  false;
    }
    
}
