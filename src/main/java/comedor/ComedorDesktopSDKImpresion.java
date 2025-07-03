/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package comedor;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import config.Config;
import config.ConfigManager;
import config.PrinterSelector;

/**
 *
 * @author lporcayo
 */
public class ComedorDesktopSDKImpresion {

    static String URL;
    static String USER;
    static String PASS;
    static int sensorID;
    // Almacena el último ID detectadomvn  
    static int ultimoId = -1;
    static String ultimoEmpleado = "0";
    static volatile boolean isRunning = false;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        new Thread(()-> listenForConfigShortcut()).start(); // Iniciar el listener en un hilo separado)
        
        // se crean las tablas de configuración e impresiones si no existen
        ConfigManager.createConfigTableIfNotExists();
        ConfigManager.createTableImpresionesIfNotExists();
        Config config = ConfigManager.loadConfig();
        System.out.println("config: "+ config);
        if (config == null) {
            cambiarConfiguracion();
            return; // Si no hay configuración, salir de la aplicación
        }
        URL = "jdbc:sqlserver://"+config.ip+":"+ config.port +";databaseName="+config.name +";encrypt=true;trustServerCertificate=true";
        USER = config.user;
        PASS = config.pass;
        sensorID = Integer.parseInt(config.sensorId);
        // obtiene el último ID de registro actual
        ultimoId = obtenerUltimoLogIdActual();
        if (ultimoId == -1) {
            mostrarMensajeAutoCierre("❗ No se pudo obtener el último registros. Asegúrate de que la base de datos esté accesible.", 3000);
            return;
        } else {
            mostrarMensajeAutoCierre("🔄 Se conectó correctamente la conexion, se obtuvo el ultimo registro: " + ultimoId, 3000);
        }
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning) {
                isRunning = true;
                try {
                    verificarNuevoRegistro();
                } finally {
                    isRunning = false;
                }
            } else {
                System.out.println("⚠️ Esperando que termine la tarea anterior...");
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    //Ventana invisible para escuchar Ctrl+Shift+C
    private static void listenForConfigShortcut() {
        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setOpacity(0f); // ventana invisible
        frame.setSize(100, 100);
        frame.setFocusable(true);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    cambiarConfiguracion();
                }
            }
        });
        frame.setVisible(true);
        frame.requestFocus();
        // Mantener el frame vivo
        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException ex) {}
        }
    }

    public static void cambiarConfiguracion() {
        // Cargar configuración previa si existe
        Config configPrevio = ConfigManager.loadConfig();
        String ip = JOptionPane.showInputDialog("IP de la base de datos:", configPrevio != null ? configPrevio.ip : "");
        String port = JOptionPane.showInputDialog("Puerto de la base de datos:", configPrevio != null ? configPrevio.port : "");
        String name = JOptionPane.showInputDialog("Nombre de la base de datos:", configPrevio != null ? configPrevio.name : "");
        String user = JOptionPane.showInputDialog("Usuario de la base de datos:", configPrevio != null ? configPrevio.user : "");
        String sensorId = JOptionPane.showInputDialog("Sensor id:", configPrevio != null ? configPrevio.sensorId : "");
        String pass = JOptionPane.showInputDialog("Contraseña de la base de datos:", configPrevio != null ? configPrevio.pass : "");
        String printer = null;
        boolean pruebaExitosa = false;
        while (!pruebaExitosa) {
            printer = PrinterSelector.selectPrinter();
            if (printer == null) {
                int opcion = JOptionPane.showConfirmDialog(null, "Debes seleccionar una impresora para continuar.\n¿Deseas cancelar la configuración?", "Impresora requerida", JOptionPane.YES_NO_OPTION);
                if (opcion == JOptionPane.YES_OPTION) {
                    mostrarMensajeAutoCierre("Configuración cancelada.", 2000);
                    return;
                }
                continue;
            }
            pruebaExitosa = PrinterSelector.testPrint(printer);
            if (!pruebaExitosa) {
                JOptionPane.showMessageDialog(null, "La prueba de impresión falló. Selecciona otra impresora.");
            }
        }

        Config config = new Config(ip, port, name, user, pass, sensorId, printer);
        ConfigManager.saveConfig(config);
        mostrarMensajeAutoCierre("Configuración actualizada. Reinicia la aplicación.", 3000);
        System.exit(0);
    }
    
    public static int obtenerUltimoLogIdActual() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT MAX(LOGID) AS maxId FROM CHECKINOUT WHERE SENSORID ="+ sensorID; // el id del comedor es el el 14 
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("maxId");
                }
            }
        } catch (SQLException e) {
            mostrarMensajeAutoCierre("Hubo un problema al obtener el ultimo registro.", 3000);
            // System.out.println("Error al obtener el último LOGID actual: " + e.getMessage());
        }
        return -1;
    }
    
    public static void verificarNuevoRegistro() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

            //String sql = "SELECT TOP 1 CHECKINOUT.CHECKTIME, CHECKINOUT.LOGID, CHECKINOUT.SENSORID, CHECKINOUT.USERID, USERINFO.USERID, USERINFO.Badgenumber, USERINFO.Name FROM CHECKINOUT JOIN USERINFO ON CHECKINOUT.USERID = USERINFO.USERID where CHECKINOUT.SENSORID = 14 ORDER BY LOGID DESC";
            String sql = "SELECT CHECKINOUT.CHECKTIME, CHECKINOUT.LOGID, CHECKINOUT.SENSORID, " +
             "CHECKINOUT.USERID, USERINFO.Badgenumber, USERINFO.Name " +
             "FROM CHECKINOUT " +
             "JOIN USERINFO ON CHECKINOUT.USERID = USERINFO.USERID " +
             "WHERE CHECKINOUT.SENSORID = "+ sensorID +" AND CHECKINOUT.LOGID > ? " +
             "ORDER BY CHECKINOUT.LOGID ASC";

            //long startTime = System.currentTimeMillis();
         
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, ultimoId);
                try (ResultSet rs = stmt.executeQuery()) {
                    //boolean hayRegistros = false;
                    while (rs.next()) {
                        System.out.println("dentro del while");
                        int nuevoId = rs.getInt("LOGID");
                        ultimoId = nuevoId; // actualizar al más alto
                        if ( !ultimoEmpleado.equals(rs.getString("Badgenumber")) ){
                            int sensor = rs.getInt("SENSORID");
                            String nombre = rs.getString("Name");
                            Timestamp fecha = rs.getTimestamp("CHECKTIME");
                            String numero_empleado = rs.getString("Badgenumber");

                            System.out.println("\nID: " + numero_empleado);
                            System.out.println("Nombre: " + nombre);
                            System.out.println("Fecha: " + fecha);
                            System.out.println("SENSOR: " + sensor);
                            
                            ultimoEmpleado = rs.getString("Badgenumber");
                            // boolean bandera = imprimirTicket(nombre, numero_empleado, fecha);
                            // if (bandera){
                                registrarImpresion(numero_empleado, fecha);
                            // }
                        }else{
                            System.out.println("No hay registros nuevos");
                        }
                    }
                }
            }catch (SQLException e) {
                mostrarMensajeAutoCierre("Error al consultar los nuevos registros", 3000);
                
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión o consulta: " + e.getMessage());
        }
    }

    public static boolean imprimirTicket(String nombre, String id, Timestamp fecha) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService selectedService = null;
        Config config = ConfigManager.loadConfig();
        
        if (config == null || config.printerName == null || config.printerName.isEmpty()) {
            mostrarMensajeAutoCierre("No hay impresora Configurada.", 2000);
            return false;
        }
        for (PrintService service : services) {
            if (service.getName().equals(config.printerName)) {
                selectedService = service;
                break;
            }
        }
        if (selectedService == null) {
            mostrarMensajeAutoCierre("No se encontró la impresora configurada: " + config.printerName, 2000);
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

                try (InputStream imgStream = ComedorDesktopSDKImpresion.class.getResourceAsStream("/resources/apollo.png")) {
                    BufferedImage logo = ImageIO.read(imgStream);
                    g2d.drawImage(logo, 30, 5, 100, 50, null);
                } catch (Exception e) {
                    // System.out.println("Error al cargar la imagen: " + e.getMessage());
                    mostrarMensajeAutoCierre("❗ Error al cargar la imagen: " + e.getMessage(), 3000);
                }

                g2d.drawString("Nombre: " + nombre, 5, 70);
                g2d.drawString("NE: " + id, 5, 90);
                g2d.drawString("Fecha: " + fecha, 5, 110);

                return Printable.PAGE_EXISTS;
            }, pageFormat);
            job.print();
            // System.out.println("Impresión realizada correctamente.");
            mostrarMensajeAutoCierre("Impresión realizada correctamente.", 3000);
            return true;

        } catch (Exception e) {
            // System.out.println("Error al imprimir: " + e.getMessage());
            mostrarMensajeAutoCierre("❗ Error al imprimir: " + e.getMessage(), 3000);
            return false;
        }
    }

    public static void mostrarMensajeAutoCierre(String mensaje, int milisegundos) {
        JOptionPane optionPane = new javax.swing.JOptionPane(mensaje, javax.swing.JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = optionPane.createDialog("Mensaje");
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        // Cierra el diálogo después del tiempo especificado
        new javax.swing.Timer(milisegundos, e -> dialog.dispose()).start();
    }

    public static void registrarImpresion(String empleado, Timestamp fecha) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "INSERT INTO impresiones (fecha, empleado, status) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, fecha);
                ps.setString(2, empleado);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // System.out.println("Error al registrar la impresión: " + e.getMessage());
            mostrarMensajeAutoCierre("❗ Error al registrar la impresión: " + e.getMessage(), 3000);
        }
    }
}
