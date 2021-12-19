package com.adobe;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

public class ScriptRunner {

    public static void main(String[] args) {
        System.out.println("Running Script.");
        final String language = args[0];
        try (Context context = Context.newBuilder()
                .allowAllAccess(true).build()) {

            if(args[1].equals("script")) {
                // js source.js flowId
                context.eval(Source.newBuilder(language, args[2], args[3])
                        .buildLiteral());
            } else {
                context.eval(Source.newBuilder(language, new File(args[1]))
                        .buildLiteral());
            }

            HashMap<String, Object> value = new HashMap<>();
            value.put("profile", Collections.singletonList("abc"));
            System.out.println(context.getBindings(language).getMember("mapping").execute(value));
//            Script script = context.getBindings("js")
//                .getMember("mapping")
//                .as(Script.class);
//            HashMap<String, Object> value = new HashMap<>();
//            value.put("profile", Collections.singletonList("abc"));
//            System.out.println(script.map(value));
        }
    }
}
