package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author lporcayo
 */
public class Config {
    
    public String ip;
    public String port;
    public String name;
    public String user;
    public String pass;
    public String sensorId;
    public String printerName;
    public String url;

    public Config(String ip, String port, String name, String user, String pass, String sensorId, String printerName) {
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.user = user;
        this.pass = pass;
        this.sensorId = sensorId;
        this.printerName = printerName;
        this.url = "jdbc:sqlserver://"+ip+":"+ port +";databaseName="+name +";encrypt=true;trustServerCertificate=true";
    }
    

    public static int obtenerUltimoLogIdActual() {
        Config config = ConfigManager.loadConfig();
        try (Connection conn = DriverManager.getConnection(config.url, config.user, config.pass)) {
            String sql = "SELECT MAX(LOGID) AS maxId FROM CHECKINOUT WHERE SENSORID ="+ config.sensorId; // el id del comedor es el el 14 
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("maxId");
                }
            }
        } catch (SQLException e) {
            Mensajes.mostrarMensajeAutoCierre("Hubo un problema al obtener el ultimo registro.", 3000);
            // System.out.println("Error al obtener el último LOGID actual: " + e.getMessage());
        }
        return -1;
    }
    
    public static RegistroInfo verificarNuevoRegistro(int ultimoId, String ultimoEmpleado) {
         Config config = ConfigManager.loadConfig();
        try (Connection conn = DriverManager.getConnection(config.url, config.user, config.pass)) {
            String sql = "SELECT CHECKINOUT.CHECKTIME, CHECKINOUT.LOGID, CHECKINOUT.SENSORID, " +
             "CHECKINOUT.USERID, USERINFO.Badgenumber, USERINFO.Name " +
             "FROM CHECKINOUT " +
             "JOIN USERINFO ON CHECKINOUT.USERID = USERINFO.USERID " +
             "WHERE CHECKINOUT.SENSORID = "+ config.sensorId +" AND CHECKINOUT.LOGID > ? ORDER BY CHECKINOUT.LOGID ASC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, ultimoId); // establecer el último ID como parámetro
                try (ResultSet rs = stmt.executeQuery()) {
                    //boolean hayRegistros = false;
                    while (rs.next()) {
                        ultimoId = rs.getInt("LOGID"); // actualizar al más alto
                        String nuevoEmpleado = rs.getString("Badgenumber");
                        Timestamp fecha = rs.getTimestamp("CHECKTIME");
                        System.out.println("nuevoEmpleado: "+ nuevoEmpleado);
                        boolean hasImpresion = ConfigManager.impresionRegistrada(nuevoEmpleado, fecha);
                        System.out.println("hasImpresion: " + hasImpresion);
                        if ( !ultimoEmpleado.equals(nuevoEmpleado) && !hasImpresion){
                            ultimoEmpleado = nuevoEmpleado; // actualizar el último empleado
                            String nombre = rs.getString("Name");
                            ultimoEmpleado = rs.getString("Badgenumber");
                            System.out.println("***************nuevo registro encontrado***************");
                            System.out.println("ne: " + ultimoEmpleado);
                            System.out.println("fecha: "+ fecha );
                            System.out.println("nombre: "+ nombre);
                            System.out.println("*******************************************************");
                            boolean bandera = Ticket.imprimirTicket(nombre, ultimoEmpleado, fecha);
                            if (bandera){
                                ConfigManager.registrarImpresion(ultimoEmpleado, fecha);
                            }
                            return new RegistroInfo(ultimoId, ultimoEmpleado);
                        }else{
                            System.out.println("No hay registros nuevos");
                            return new RegistroInfo(ultimoId, ultimoEmpleado);
                        }
                    }
                }catch (Exception e){
                    System.out.println("error:" + e.getMessage());
                }
            }catch (SQLException e) {
                Mensajes.mostrarMensajeAutoCierre("Error al consultar los nuevos registros", 3000);
                
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión o consulta: " + e.getMessage());
        }
        return new RegistroInfo(ultimoId, ultimoEmpleado);
    }
    
}
