package OPT_UI;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Fogim
 */

import com.google.common.eventbus.Subscribe;
import javax.swing.JFrame;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

@Plugin(type = MenuPlugin.class)
public class OPT_main  implements MenuPlugin, SciJavaPlugin{
    //Name for the plugin
    public static final String menuName = "OPT for MM2";
    private Studio gui_;    
    public static JFrame frame_;
    
    @Override
    public String getSubMenu() {
        return("OPT");
    }

    @Override
    public void onPluginSelected() {
        System.out.println("Starting OPT plugin for Micro-manager 2");
        frame_ = new OPT_UI.OPT_hostframe(gui_);
        frame_.show();
    }

    @Override
    public void setContext(Studio studio) {
        gui_ = studio;
    }

    @Override
    public String getName() {
        return menuName;
    }

    @Override
    public String getHelpText() {
        return("Sorry!");
    }

    @Override
    public String getVersion() {
        return ("0.0.1");
    }

    @Override
    public String getCopyright() {
        return ("Copyright Imperial College London[2018]");
    }
    
}
