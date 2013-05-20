/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.game.runner.game.element.level;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.IEntity;
import org.andengine.entity.shape.Shape;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.color.Color;
import org.game.runner.game.player.Player;

/**
 *
 * @author Karl
 */
public abstract class LevelElement{
    public static final int BONUS_HEIGHT = 30;
    public static final int BONUS_WIDTH = 30;
    public static final int TRAP_HEIGHT = 30;
    public static final int TRAP_WIDTH = 30;
    public static final int PLATFORM_THICKNESS = 10;
    public static final int PLATFORM_WIDTH = 50;
    
    private Body body;
    private Shape shape;
    
    public void build(float pX, float pY, VertexBufferObjectManager pVertexBufferObjectManager, final Player player, PhysicsWorld physicWorld){
        this.shape = this.buildShape(pX, pY, pVertexBufferObjectManager, player);
        this.shape.setColor(this.getColor());
        this.body = PhysicsFactory.createBoxBody(physicWorld, this.shape, BodyDef.BodyType.KinematicBody, PhysicsFactory.createFixtureDef(0, 0, 0));
        this.body.setFixedRotation(true);
        physicWorld.registerPhysicsConnector(new PhysicsConnector(this.shape, this.body, true, false));
    }
    public Body getBuildedBody(){
        return this.body;
    }
    public Shape getBuildedShape(){
        return this.shape;
    }
    
    public abstract Color getColor();
    protected abstract Shape buildShape(float pX, float pY, VertexBufferObjectManager pVertexBufferObjectManager, final Player player);
    public abstract void playerAction(Player player);
}
