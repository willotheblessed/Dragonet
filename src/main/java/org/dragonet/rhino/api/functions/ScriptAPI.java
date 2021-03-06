 /* GNU LESSER GENERAL PUBLIC LICENSE
 *                       Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view LICENCE file for details. 
 */
package org.dragonet.rhino.api.functions;

import java.util.ArrayList;

import org.dragonet.DragonetServer;
import org.dragonet.rhino.CustomMethod;
import org.dragonet.rhino.Script;
import org.dragonet.rhino.ScriptCommand;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.annotations.JSFunction;

/**
 *
 * @author Ash (QuarkTheAwesome)
 */
public class ScriptAPI extends ScriptableObject {

    private static final long serialVersionUID = 438270592527335642L;

    //Rhino compatibility stuff
    public ScriptAPI() {
    }

    @Override
    public String getClassName() {
        return "ScriptAPI";
    }

    ////////////////
    //
    // Cross-Script Methods
    //
    ////////////////
    @JSFunction
    public static void callCustomFunction(String function, Object arguments) {
        if (!(arguments == null)) {
            for (Script s : DragonetServer.instance().getRhino().getScripts()) {
                s.runFunction(function, new Object[]{arguments});
            }
        } else {
            for (Script s : DragonetServer.instance().getRhino().getScripts()) {
                s.runFunction(function, new Object[]{null});
            }
        }
    }

    @JSFunction
    public static void addMethod(String method, String handler, String ownerUID) {
        //Check if ownerUID belongs to a valid script
        Script scr = null;
        try {
            for (Script s : DragonetServer.instance().getRhino().getScripts()) {
                if (s.getUID().equals(ownerUID)) {
                    scr = s;
                }
            }
        } catch (WrappedException e) {
            DragonetServer.instance().getLogger().error("[DragonetAPI] Script tried to add a method before initialization finished! Please use postInit for this.");
        }

        if (scr == null) {
            DragonetServer.instance().getLogger().error("[DragonetAPI] Script doesn't have a valid UID but is trying to register method " + method + "! Received '" + ownerUID + "', this does not belong to any script!");
            DragonetServer.instance().getLogger().error("[DragonetAPI] Method " + method + " will not be defined. This will cause issues with other scripts.");
            return;
        }

        //Check if method name is already taken
        for (CustomMethod m : CustomMethod.methods) {
            if (m.method.equals(method)) {
                DragonetServer.instance().getLogger().error("[DragonetAPI] Script " + scr.getUID() + " (" + scr.getName() + ")" + " tried to reserve method " + method + ", but this has already been reserved by " + m.owner.getName() + "!");
                DragonetServer.instance().getLogger().error("[DragonetAPI] Method " + method + " will not be defined. This will cause issues with other scripts.");
            }
        }

        //Finally, add method
        CustomMethod.methods.add(new CustomMethod(method, handler, scr));
        DragonetServer.instance().getLogger().info("[DragonetAPI] Script " + scr.getUID() + " (" + scr.getName() + ") added API method " + method + " sucessfully.");
    }

    @JSFunction
    public static void callCustomMethod(String method, Object arguments) {
        //Cycle through all scripts looking for our method.
        for (CustomMethod m : CustomMethod.methods) {
            if (m.method.equals(method)) {
                //Found it! Run.
                m.run(new Object[]{arguments});
            }
        }
    }

    @JSFunction
    public static boolean registerCommand(final Object script, final String commandName, String handlerFunction) {
        return registerCommandWithPerms(script, commandName, handlerFunction, "");
    }

    @JSFunction
    public static boolean registerCommandWithPerms(final Object script, final String commandName, String handlerFunction, final String requiredPermissions){
        if(!Script.class.isInstance(script)){
            return false;
        }
        DragonetServer.instance().getServer().getCommandMap().register(commandName, "[" + ((Script)script).getName() + ":" + commandName + "]", new ScriptCommand(commandName, (Script)script, handlerFunction, requiredPermissions));
        return true;
    }
    
    public static void resetMethods() {
        DragonetServer.instance().getLogger().info("[DragonetAPI] Removing all custom methods...");
        CustomMethod.methods = new ArrayList<>();
    }
}
