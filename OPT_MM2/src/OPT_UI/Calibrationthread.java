/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package OPT_UI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kumars2
 */
public class Calibrationthread implements Runnable{
    private OPT_hostframe frame;
    
    public Calibrationthread(OPT_hostframe frame_){
        frame = frame_;
    }

    @Override
    public void run() {
        try {        
            frame.run_calibration();
        } catch (Exception ex) {
            System.out.println("Calibration Thread Failed!");
            Logger.getLogger(Calibrationthread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
