package com.example.cruzr.robot;

import android.util.Log;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

public class RobotCommandInvoker {

    private static RobotCommandInvoker instance;
    private final Map<String, RobotCommand> commandRegistry  = new HashMap<String, RobotCommand>() {{
        put("move", new MoveCommand());
        put("preset", new PresetCommand());
        put("expression", new ExpressionCommand());
        put("navigation", new NavigationCommand());
    }};

    private RobotCommandInvoker() {}

    public static synchronized RobotCommandInvoker getInstance() {
        if (instance == null) {
            instance = new RobotCommandInvoker();
        }
        return instance;
    }

    public void execute(String type, JSONArray parameters) {
        if (commandRegistry.containsKey(type)) {
            commandRegistry.get(type).execute(parameters);
        } else {
            Log.e("API", "Invalid keyword for action type");
        }
    }
}
