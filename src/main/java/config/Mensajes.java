/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package config;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 *
 * @author lporcayo
 */
public class Mensajes {

    public static void mostrarMensajeAutoCierre(String mensaje, int milisegundos) {
        JOptionPane optionPane = new javax.swing.JOptionPane(mensaje, javax.swing.JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = optionPane.createDialog("Mensaje");
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        // Cierra el diálogo después del tiempo especificado
        new javax.swing.Timer(milisegundos, e -> dialog.dispose()).start();
    }

}
