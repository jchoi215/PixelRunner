/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.game.runner.scene;

import android.util.Log;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.andengine.entity.modifier.MoveModifier;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.scene.background.ParallaxBackground;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.extension.physics.box2d.FixedStepPhysicsWorld;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.util.adt.color.Color;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.IModifier;
import org.game.runner.base.BaseScene;
import org.game.runner.game.LevelDescriptor;
import org.game.runner.game.element.BackgroundElement;
import org.game.runner.game.element.LevelElement;
import org.game.runner.game.player.Player;
import org.game.runner.manager.AudioManager;
import org.game.runner.manager.SceneManager;
import org.game.runner.manager.SceneManager.SceneType;
import org.game.runner.utils.EaseBroadcast;

/**
 *
 * @author Karl
 */
public class GameLevelScene extends BaseScene implements IOnSceneTouchListener{
    private final float LEFT_LIMIT = 20;
    private final float RIGHT_LIMIT = 780;
    private final float GROUND_LEVEL = 50;
    private final float GROUND_WIDTH = 1000;
    private final float GROUND_THICKNESS = 10;
    private final float BROADCAST_LEVEL = 240;
    private final float BROADCAST_LEFT = -100;
    private final float BROADCAST_RIGHT = 1000;
    
    //Background
    private AutoParallaxBackground autoParallaxBackground;
    private List<Sprite> backgroundParallaxLayers = new LinkedList<Sprite>();
    
    //Broadcast
    private Text chrono3;
    private Text chrono2;
    private Text chrono1;
    private Text chronoStart;
    
    //Graphics
    private Player player;
    private Rectangle ground;
    
    //Physic
    private PhysicsWorld physicWorld;
    private Body groundBody;
    
    //Level
    private LevelDescriptor level;
    private TimerHandler levelReaderHandler;
    private ITimerCallback levelReaderAction;
    
    //Detectors
    private ConcurrentLinkedQueue<IEntity> levelElements = new ConcurrentLinkedQueue<IEntity>();
    private Line removerLeft;
    private Line removerRight;
    
    //Gameplay
    private int jump = 0;
    
    public GameLevelScene(LevelDescriptor level){
        this.level = level;
        this.createBackground();
        this.createLevelSpwaner();
    }
    
    @Override
    public void createScene() {
        
        //Broadcast messages
        this.chrono3 = new Text(0, 0, resourcesManager.fontPixel_200, "3", vbom);
        this.chrono2 = new Text(0, 0, resourcesManager.fontPixel_200, "2", vbom);
        this.chrono1 = new Text(0, 0, resourcesManager.fontPixel_200, "1", vbom);
        this.chronoStart = new Text(0, 0, resourcesManager.fontPixel_200, "GO !", vbom);
        this.chrono3.setVisible(false);
        this.chrono2.setVisible(false);
        this.chrono1.setVisible(false);
        this.chronoStart.setVisible(false);
        attachChild(this.chrono3);
        attachChild(this.chrono2);
        attachChild(this.chrono1);
        attachChild(this.chronoStart);
        
        //Physics engine
        this.physicWorld = new FixedStepPhysicsWorld(60, new Vector2(0, -50), false);
        this.physicWorld.setContactListener(this.contactListener());
        this.registerUpdateHandler(this.physicWorld);
        final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0, 0);
        
        //Ground
        this.ground = new Rectangle(400, GROUND_LEVEL, GROUND_WIDTH, GROUND_THICKNESS, this.vbom);
        attachChild(this.ground);
	this.groundBody = PhysicsFactory.createBoxBody(this.physicWorld, this.ground, BodyDef.BodyType.StaticBody, wallFixtureDef);
        
        //Detectors
        this.removerLeft = new Line(LEFT_LIMIT, 0, LEFT_LIMIT, 480, vbom);
        this.removerLeft.setColor(Color.RED);
        attachChild(this.removerLeft);
        this.removerRight = new Line(RIGHT_LIMIT, 0, RIGHT_LIMIT, 480, vbom);
        this.removerRight.setColor(Color.RED);
        attachChild(this.removerRight);
        
        //Player
        this.player = new Player(400, GROUND_LEVEL + GROUND_THICKNESS/2 + 32, this.resourcesManager.player, this.vbom, this.physicWorld);
        attachChild(this.player);
        
        this.setOnSceneTouchListener(this);
    }
    private void createBackground(){
        this.autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
        this.setBackground(this.autoParallaxBackground);
        Debug.d("PixelRunnerGameLevelScene", "Backgrounds to place : " + level.getBackgrounds());
        for(BackgroundElement layer : this.level.getBackgrounds()){
            Sprite layerSprite = new Sprite(layer.x, layer.y, this.resourcesManager.gameParallaxLayers.get(layer.getResourceName()), this.vbom);
            this.backgroundParallaxLayers.add(layerSprite);
            layerSprite.setOffsetCenter(0, 0);
            this.autoParallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(-layer.speed, layerSprite));
        }
    }
    private void createLevelSpwaner(){
        //Level elements
        this.levelReaderAction = new ITimerCallback(){                      
            @Override
            public void onTimePassed(final TimerHandler pTimerHandler){
                if(!GameLevelScene.this.level.hasNext()){
                    //End of level
                    GameLevelScene.this.unregisterUpdateHandler(GameLevelScene.this.levelReaderHandler);
                }
                else{
                    //Level elements spawn
                    LevelElement next = GameLevelScene.this.level.getNext();
                    switch(next.getType()){
                        case TRAP_JUMP:
                            final float trapY = GROUND_LEVEL + GROUND_THICKNESS/2 + GameLevelScene.this.resourcesManager.test.getHeight()/2;
                            Sprite trap = new Sprite(800, trapY, GameLevelScene.this.resourcesManager.test, GameLevelScene.this.vbom){
                                @Override
                                protected void onManagedUpdate(float pSecondsElapsed){
                                    super.onManagedUpdate(pSecondsElapsed);
                                    if(this.collidesWith(GameLevelScene.this.player)){
                                        Log.d("PixelRunner", "Player hit");
                                        GameLevelScene.this.restart();
                                    }
                                }
                            };
                            GameLevelScene.this.levelElements.add(trap);
                            Log.d("PixelRunner", "Spawn element");
                            GameLevelScene.this.attachChild(trap);
                            //trap.registerEntityModifier(new MoveModifier(0.5f, RIGHT_LIMIT, trapY, LEFT_LIMIT, trapY));
                            PhysicsHandler handler = new PhysicsHandler(trap);
                            trap.registerUpdateHandler(handler);
                            handler.setVelocity(-GameLevelScene.this.level.getSpawnSpeed(), 0);
                            break;
                    }
                }
            }
        };
        
        //Detectors checking
        this.registerUpdateHandler(new IUpdateHandler() {
            @Override
            public void reset() { }

            @Override
            public void onUpdate(final float pSecondsElapsed) {
                //Level elements unspawn
                for(IEntity element : GameLevelScene.this.levelElements){
                    if(element.collidesWith(GameLevelScene.this.removerLeft)){
                        Log.d("PixelRunner", "Left unspawn");
                        GameLevelScene.this.levelElements.remove(element);
                        element.clearUpdateHandlers();
                        element.detachSelf();
                        element.dispose();
                    }
                }
            }
        });
    }
    
    private void restart(){
        Log.d("PixelRunner", "Restart");
        if(!this.activity.isMute()){
            this.engine.vibrate(100);
        }
        AudioManager.getInstance().stop();
        this.unregisterUpdateHandler(this.levelReaderHandler);
        this.disposeLevelElements();
        this.start();
    }
    
    public void start(){
        Log.d("PixelRunner", "Start");
        this.level.start();
        
        this.broadcast(this.chrono3, new IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
            }
            
            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                 GameLevelScene.this.broadcast(GameLevelScene.this.chrono2, new IEntityModifierListener() {
                    @Override
                    public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                    }
                    
                    @Override
                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                        GameLevelScene.this.broadcast(GameLevelScene.this.chrono1, new IEntityModifierListener() {
                            @Override
                            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                            }
                            
                            @Override
                            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                                GameLevelScene.this.broadcast(GameLevelScene.this.chronoStart, new IEntityModifierListener() {
                                    @Override
                                    public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                                        AudioManager.getInstance().play("mfx/", GameLevelScene.this.level.getMusic());
                                        GameLevelScene.this.registerUpdateHandler(GameLevelScene.this.levelReaderHandler = new TimerHandler(GameLevelScene.this.level.getSpawnTime(), true, GameLevelScene.this.levelReaderAction));
                                    }
                                    
                                    @Override
                                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                                    }
                                }); 
                            }
                        });
                    }
                });
            }
        });
    }
    
    private void broadcast(final IEntity entity, final IEntityModifierListener listener){
        entity.registerEntityModifier(new MoveModifier(0.5f, BROADCAST_RIGHT, BROADCAST_LEVEL, BROADCAST_LEFT, BROADCAST_LEVEL, new IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                entity.setVisible(true);
                listener.onModifierStarted(pModifier, pItem);
            }
            
            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                entity.setVisible(false);
                listener.onModifierFinished(pModifier, pItem);
            }
        }, EaseBroadcast.getInstance()));
    }

    @Override
    public void onBackKeyPressed() {
        AudioManager.getInstance().stop();
        SceneManager.getInstance().unloadGameLevelScene();
    }

    @Override
    public void onPause() {
        this.audioManager.pause();
    }

    @Override
    public void onResume() {
        this.audioManager.resume();
    }

    @Override
    public SceneType getSceneType() {
        return SceneType.SCENE_GAME_LEVEL;
    }
    
    private void destroyPhysicsWorld(){
        this.engine.runOnUpdateThread(new Runnable(){
            public void run(){
                PhysicsWorld world = GameLevelScene.this.physicWorld;
                Iterator<Body> localIterator = GameLevelScene.this.physicWorld.getBodies();
                while (true){
                    if (!localIterator.hasNext()){
                        world.clearForces();
                        world.clearPhysicsConnectors();
                        world.reset();
                        world.dispose();
                        System.gc();
                        return;
                    }
                    try{
                        final Body localBody = (Body) localIterator.next();
                        world.destroyBody(localBody);
                    }
                    catch (Exception localException){
                        Debug.e(localException);
                    }
                }
            }
        });
    }
    
    private void disposeLevelElements(){
        for(IEntity element : GameLevelScene.this.levelElements){
            GameLevelScene.this.levelElements.remove(element);
            element.clearUpdateHandlers();
            element.detachSelf();
            element.dispose();
        }
    }

    @Override
    public void disposeScene() {
        this.destroyPhysicsWorld();
        this.disposeLevelElements();
        this.chrono3.detachSelf();
        this.chrono3.dispose();
        this.chrono2.detachSelf();
        this.chrono2.dispose();
        this.chrono1.detachSelf();
        this.chrono1.dispose();
        this.chronoStart.detachSelf();
        this.chronoStart.dispose();
        this.ground.detachSelf();
        this.ground.dispose();
        this.player.detachSelf();
        this.player.dispose();
        for(Sprite layer : this.backgroundParallaxLayers){
            layer.detachSelf();
            layer.dispose();
        }
        this.removerLeft.detachSelf();
        this.removerLeft.dispose();
        this.removerRight.detachSelf();
        this.removerRight.dispose();
        this.detachSelf();
        this.dispose();
    }

    public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
        if (pSceneTouchEvent.isActionDown()){
            //Jump
            if(this.jump < 2){
                this.player.jump();
            }
        }
        return false;
    }
    
    private ContactListener contactListener(){
        ContactListener contactListener = new ContactListener(){
            @Override
            public void beginContact(Contact contact){
                final Fixture xA = contact.getFixtureA();
                final Fixture xB = contact.getFixtureB();
                //Player contacts
                if (xA.getBody().equals(GameLevelScene.this.player.getBody()) || xB.getBody().equals(GameLevelScene.this.player.getBody())){
                    GameLevelScene.this.player.onGround();
                }
            }

            @Override
            public void endContact(Contact contact){
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold){
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse){
            }
        };
        return contactListener;
    }
}
