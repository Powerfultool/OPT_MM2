/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package OPT_UI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import org.micromanager.data.ImageJConverter;

import javax.swing.JOptionPane;
import mmcorej.TaggedImage;

import org.micromanager.data.Coords;
import org.micromanager.data.Image;
import org.micromanager.data.Datastore;
import org.micromanager.data.Metadata;
import org.micromanager.data.ImageJConverter;
import org.micromanager.data.RewritableDatastore;
import org.micromanager.display.DisplaySettings;
import org.micromanager.display.DisplayWindow;
//import org.micromanager.display.DisplaySettings.DisplaySettingsBuilder;
import org.micromanager.internal.utils.ImageUtils;

/**
 *
 * @author Fogim
 */
public class OPT_hostframe extends javax.swing.JFrame {
    static OPT_hostframe frame_;  
    public static Studio gui_ = null;
    private CMMCore core_ = null;   
    Gson gson = new GsonBuilder().create();
    FileWriter fw;
    boolean aborted_ = false;
    boolean calib_running = false;
    DisplayWindow calib_display;
    ImagePlus calib_implus;
    ImageProcessor improc;
    ColorProcessor calib_RGBproc;
    ShortProcessor calib_shortproc;
    ByteProcessor calib_byteproc;
    String rotstagename = "RotStage";
    Datastore acq_store = null;

    public Thread calibThread;
    
    /**
     * Creates new form OPT_hostframe
     */
    public OPT_hostframe(Studio gui_ref) {
        frame_ = this;            
//        frame_.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("HCAicon.png")));
//        frame_.setTitle("OpenHCA controller for Micro-manager 2");        
        frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame_.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                if (true == confirmQuit()){
                    frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                } else {
                    frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });           
        gui_ = gui_ref;
        core_ = gui_.getCMMCore();
        initComponents();
        //Pass reference to this frame to children to allow callbacks
        rotation_control1.initialise(frame_);
        save_details1.initialise(frame_);
        abort1.initialise(frame_);
        
        try {
            core_.setExposure(100.0);
        } catch (Exception ex) {
            Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
        gui_.live().setLiveMode(true);
        
        //calib_implus = new ImageProcessor();
    }
    
    private boolean confirmQuit() {
        int n = JOptionPane.showConfirmDialog(frame_,
                "Quit: are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
        if (n == JOptionPane.YES_OPTION) {
            return true;
        }
            return false;
    }
    
    public void abort(){
        calib_running = false;
        progress_indicator1.set_working(false);
        //Set to 100%, or leave at progress when aborted?
        //progress_indicator1.set_progress(100);
        progress_indicator1.set_aborted(true);
        aborted_ = true;
        //Maybe go live?
        gui_.live().setLiveMode(true);
        calib_display.requestToClose();
        //###Reenable acq and calib buttons here
    }
    
    public void reset_abort(){
        set_working(false);
        progress_indicator1.set_aborted(false);
        aborted_ = false;
    }
    
    public void set_working(boolean working){
        progress_indicator1.set_progress(0);
        progress_indicator1.set_working(working);
    }

    public void run_acquisition_threaded() throws Exception{
        Thread acqThread = new Thread(new Runnable() {
               @Override
               public void run() {
                   try {
                       run_acquisition();
                   } catch (Exception ex) {
                       Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
                   }
               }
            });
        acqThread.start();     
    }
    
    public void run_acquisition() throws Exception{
        //Make sure we can press the abort button...
        reset_abort();
        //###NEED TO FIX THE FORCED-PATH CASE
        String basedir = save_details1.get_savepath();
        String samplename = save_details1.get_samplename();
        String savedetails = save_details1.get_savechoice();
        System.out.println(savedetails);
        String filterset = "XFP";
        String fullpath = "";
        if (savedetails.equalsIgnoreCase("Auto-path")){
            fullpath = basedir+"\\"+samplename+"\\"+filterset;
        } else {
            System.out.println("Forcepath");
        }
        String append = "\\X";
        File saveloc = new File(fullpath);
//        while(saveloc.exists()){
//            System.out.println(fullpath);
//            if (savedetails.equalsIgnoreCase("Auto-path")){
//                fullpath = basedir+"\\"+samplename+"\\"+filterset+append;
//            } else {
//                fullpath = basedir+append;
//            }
//            append = append+"X";
//            saveloc = new File(fullpath);
//            saveloc.mkdirs();
//        }
        if(saveloc.exists()){
            String titleBar = "ABORTING!";
            String infoMessage = "FILE EXISTS!";
            JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
            abort();
        }
        int numproj = rotation_control1.get_numproj();
        //Kill live mode if running
        gui_.live().setLiveMode(false);
        //Setup a datastore
        acq_store = gui_.data().createSinglePlaneTIFFSeriesDatastore(fullpath);
        DisplayWindow OPT_display = gui_.displays().createDisplay(acq_store);
        gui_.displays().manage(acq_store);
        //Setup co-ordinates
        Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
        builder.z(numproj);
        Coords coords = builder.build();      

        Image curr_img;
        int stepsize = (rotation_control1.zdist_per_revolution/rotation_control1.get_numproj());       
        for(int pos = 0; pos<numproj; pos++){
            if(aborted_){
                break;
            } else {
                coords = coords.copy().z(pos).build();
                double oldpos = core_.getPosition(rotstagename);
                core_.snapImage();
                core_.setPosition(rotstagename,oldpos+stepsize);
                //convertTaggedImage takes (IMG/COORDS/METADATA)
                curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);
                Image newimg = curr_img.copyAtCoords(coords);
                acq_store.putImage(curr_img);
                progress_indicator1.set_progress((int) (100*(((double)pos+1.0)/(double)numproj)));
                core_.waitForDevice(rotstagename);
            }
        }
        String titleBar = "Threaded popup";
        String infoMessage = "Runs after the wimdow should appear...";
        JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
        gui_.live().setLiveMode(true);
    }
            
    public void run_calibration_threaded() {                                                    
    // starts sequence in new thread
        calibThread = new Thread(new Calibrationthread(this));
        calibThread.start();
    }      
    
    public void run_test(){
        gui_.live().setLiveMode(false);
        int imwidth = (int)core_.getImageWidth();
        int imheight = (int)core_.getImageHeight();
        set_working(true);        
        try {
            core_.snapImage();
        } catch (Exception ex) {
            Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Exception caught on snap", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }
        Image curr_img = null;
        Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
        builder.channel(2);
        Coords coords = builder.build();      
        try {
            curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);
            JOptionPane.showMessageDialog(null, "Img size: "+imwidth+","+imheight, "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Exception caught on show", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }
//        try {
            calib_shortproc = new ShortProcessor(imwidth,imheight);
//        } catch (Exception ex) {
//            JOptionPane.showMessageDialog(null, "Exception caught on create processor", "Oops!", JOptionPane.INFORMATION_MESSAGE);
//        }
//        try {
            calib_shortproc.setPixels(curr_img.getRawPixels());
//        } catch (Exception ex) {
//            JOptionPane.showMessageDialog(null, "Exception caught on set pixels", "Oops!", JOptionPane.INFORMATION_MESSAGE);
//        }        
        try {
            //calib_implus.
            //calib_implus.setProcessor("LALALA",calib_shortproc);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Exception caught on set processor", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }
        try {
            calib_implus.updateAndDraw();        
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Exception caught on draw", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }        
        abort();
    }
    
    WindowListener listener = new WindowAdapter() {
        public void windowClosing(WindowEvent w) {
            abort();
        }
    };
    
    public void run_calibration() throws Exception{
        if(!calib_running){
            gui_.live().setLiveMode(false);
            set_working(true);
            RewritableDatastore cal_store = gui_.data().createRewritableRAMDatastore();        
            calib_display = gui_.displays().createDisplay(cal_store);

            //listener is still a bit iffy
            calib_display.getAsWindow().addWindowListener(listener);

            Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
            builder = builder.time(0).channel(0);
            Image image = gui_.live().snap(false).get(0);
            Image reflected = image.copyAtCoords(image.getCoords());
            ImageProcessor flipper = gui_.data().ij().createProcessor(reflected);
            int l_r = 0;
            double oldpos = core_.getPosition();
            while(aborted_ == false){
                core_.waitForDevice(rotstagename);
                oldpos = core_.getPosition();
                int chan_num = l_r%2;
                builder.channel(chan_num);
                image = gui_.live().snap(false).get(0);
                image = image.copyAtCoords(builder.build());               
                if (chan_num == 0){
                    cal_store.putImage(image);
                } else {
                    //Have to use getRawPixelsCopy, as otherwise it doesn't work
                    //Something to do with Image 'immutability' if it's passing a reference maybe?
                    reflected = image.copyAtCoords(builder.build());
                    flipper.setPixels(reflected.getRawPixelsCopy());
                    if (rotation_control1.is_axis_horizontal()){
                        flipper.flipVertical();
                    } else {
                        flipper.flipHorizontal();
                    }
                    reflected = gui_.data().ij().createImage(flipper, builder.build(), image.getMetadata());                    
                    cal_store.putImage(reflected);
                }
                l_r +=1;
                core_.setPosition(oldpos+(rotation_control1.zdist_per_revolution/2));
            }            
            cal_store.deleteAllImages();
            calib_display.forceClosed();
            reset_abort();
        } else {
            JOptionPane.showMessageDialog(null, "System already in calibration mode!", "Hang on!", JOptionPane.INFORMATION_MESSAGE);
            abort();
        }        
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rotation_control1 = new OPT_UI.Rotation_control();
        save_details1 = new OPT_UI.Save_details();
        abort1 = new OPT_UI.Abort();
        progress_indicator1 = new OPT_UI.progress_indicator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(save_details1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(rotation_control1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(progress_indicator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(abort1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)))
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rotation_control1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(abort1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(progress_indicator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(save_details1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OPT_hostframe(gui_).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private OPT_UI.Abort abort1;
    private OPT_UI.progress_indicator progress_indicator1;
    private OPT_UI.Rotation_control rotation_control1;
    private OPT_UI.Save_details save_details1;
    // End of variables declaration//GEN-END:variables
}
