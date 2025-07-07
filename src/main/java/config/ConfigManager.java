/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.swing.JOptionPane;

/**
 *
 * @author lporcayo
 */
public class ConfigManager {

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASS = "";
    
    private static final String DB_NAME = "comedorDesktop";
    private static final String CONFIG_TABLE = "config";
    
    public static void createConfigTableIfNotExists() {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS)) {
            conn.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (Connection conn = DriverManager.getConnection(MYSQL_URL + DB_NAME, MYSQL_USER, MYSQL_PASS)) {
            String sql = "CREATE TABLE IF NOT EXISTS " + CONFIG_TABLE + " (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "ip VARCHAR(100), "
                    + "port VARCHAR(100), "
                    + "dbName VARCHAR(100),"
                    + "dbUser VARCHAR(100), "
                    + "dbPass VARCHAR(100), "
                    + "sensorId VARCHAR(4), "
                    + "printerName VARCHAR(100))";
            conn.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void createTableImpresionesIfNotExists() {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL + DB_NAME, MYSQL_USER, MYSQL_PASS)) {
            String sql = "CREATE TABLE IF NOT EXISTS impresiones (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "fecha DATETIME, "
                    + "empleado VARCHAR(100))";
            conn.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static Config loadConfig() {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL + DB_NAME, MYSQL_USER, MYSQL_PASS)) {
            String sql = "SELECT ip, port, dbName, dbUser, dbPass, sensorId, printerName FROM " + CONFIG_TABLE + " LIMIT 1";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                return new Config(
                        rs.getString("ip"),
                        rs.getString("port"),
                        rs.getString("dbName"),
                        rs.getString("dbUser"),
                        rs.getString("dbPass"),
                        rs.getString("sensorId"),
                        rs.getString("printerName")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveConfig(Config config) {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL + DB_NAME, MYSQL_USER, MYSQL_PASS)) {
            // Borra la configuración anterior (si existe)
            conn.createStatement().executeUpdate("DELETE FROM " + CONFIG_TABLE);
            String sql = "INSERT INTO " + CONFIG_TABLE + " (ip, port, dbName, dbUser, dbPass, sensorId, printerName) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, config.ip);
            ps.setString(2, config.port);
            ps.setString(3, config.name);
            ps.setString(4, config.user);
            ps.setString(5, config.pass);
            ps.setString(6, config.sensorId);
            ps.setString(7, config.printerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    

    public static void registrarImpresion(String empleado, Timestamp fecha) {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL + DB_NAME, MYSQL_USER, MYSQL_PASS)) {
            String sql = "INSERT INTO impresiones (fecha, empleado) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, fecha);
                ps.setString(2, empleado);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // System.out.println("Error al registrar la impresión: " + e.getMessage());
            Mensajes.mostrarMensajeAutoCierre("❗ Error al registrar la impresión: " + e.getMessage(), 3000);
        }
    }

    public static boolean impresionRegistrada(String empleado, Timestamp fecha) {
        System.out.println("empleado: " + empleado + ", fecha: " + fecha);
        try (Connection conn = DriverManager.getConnection(MYSQL_URL + DB_NAME, MYSQL_USER, MYSQL_PASS)) {
            String sql = "SELECT COUNT(*) FROM impresiones WHERE empleado = ? AND DATE(fecha) = DATE(?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, empleado);
                ps.setTimestamp(2, fecha);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            Mensajes.mostrarMensajeAutoCierre("❗ Error al verificar la impresión: " + e.getMessage(), 3000);
        }
        return false;
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
                    Mensajes.mostrarMensajeAutoCierre("Configuración cancelada.", 2000);
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
        Mensajes.mostrarMensajeAutoCierre("Configuración actualizada. Reinicia la aplicación.", 3000);
        System.exit(0);
    }
    
    
}
