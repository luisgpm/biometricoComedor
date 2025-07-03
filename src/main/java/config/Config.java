package config;

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

    public Config(String ip, String port, String name, String user, String pass, String sensorId, String printerName) {
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.user = user;
        this.pass = pass;
        this.sensorId = sensorId;
        this.printerName = printerName;
    }
    
    
    
}
