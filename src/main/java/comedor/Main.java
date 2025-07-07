/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package comedor;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import config.Config;
import config.ConfigManager;
import config.Mensajes;
import config.RegistroInfo;

/**
 *
 * @author lporcayo
 */
public class Main {

    public static String ultimoEmpleado = "0";
    public static int ultimoId = -1;
    static volatile boolean isRunning = false;


    public static void main(String[] args) {

        new Thread(()-> listenForConfigShortcut()).start(); // Iniciar el listener en un hilo separado)

        Config config = ConfigManager.loadConfig();
        if (config == null) {
            ConfigManager.cambiarConfiguracion();
            return; // Si no hay configuración, salir de la aplicación
        }

        ConfigManager.createConfigTableIfNotExists();
        ConfigManager.createTableImpresionesIfNotExists();

        ultimoId = Config.obtenerUltimoLogIdActual();
        if (ultimoId == -1) {
            Mensajes.mostrarMensajeAutoCierre("❗ No se pudo obtener el último registros. Asegúrate de que la base de datos esté accesible.", 3000);
            return;
        } else {
            Mensajes.mostrarMensajeAutoCierre("🔄 Se conectó correctamente la conexion, se obtuvo el ultimo registro: " + ultimoId, 3000);
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning) {
                isRunning = true;
                try {
                    System.out.println("se verifica nuevo registro");
                    RegistroInfo info = Config.verificarNuevoRegistro(ultimoId, ultimoEmpleado);
                    ultimoId = info.ultimoId;
                    ultimoEmpleado = info.ultimoEmpleado;
                    //System.out.println("ultimoId: " + ultimoId + ", ultimoEmpleado: " + ultimoEmpleado);
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
                    ConfigManager.cambiarConfiguracion();
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

}
