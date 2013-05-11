/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.game.runner.manager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.andengine.engine.Engine;
import org.andengine.util.FileUtils;
import org.game.runner.GameActivity;
import org.helllabs.android.xmp.ModPlayer;

/**
 *
 * @author Karl
 */
public class AudioManager {
    private static final AudioManager INSTANCE = new AudioManager();
    
    private Engine engine = ResourcesManager.getInstance().engine;
    private GameActivity activity = ResourcesManager.getInstance().activity;
    
    private final ModPlayer mModPlayer = ModPlayer.getInstance();
    private boolean looping = true;
    
    public void prepareModFile(final String dir, final String file){
        if(FileUtils.isExternalStorageReadable() && FileUtils.isExternalStorageWriteable() && !FileUtils.isFileExistingOnExternalStorage(this.activity, dir + file)) {
            AudioManager.this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileUtils.ensureDirectoriesExistOnExternalStorage(AudioManager.this.activity, dir);
                        FileUtils.copyToExternalStorage(AudioManager.this.activity, dir + file, dir + file);
                    } catch (IOException ex) {
                        Logger.getLogger(GameActivity.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }
    
    public void playModFile(final String dir, final String file){
        if(FileUtils.isExternalStorageReadable()){
            if(!FileUtils.isFileExistingOnExternalStorage(this.activity, dir + file)){
                this.prepareModFile(dir, file);
            }
            this.mModPlayer.play(FileUtils.getAbsolutePathOnExternalStorage(this.activity, dir + file), this.looping);
        }
    }
    
    public void setLooping(boolean looping){
        this.looping = looping;
    }
    
    public static AudioManager getInstance(){
        return INSTANCE;
    }
}
