package com.adobe;

import com.adobe.http.HttpRequester;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class ScriptRunnerTest {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("Running Script.");
        final String language = args[0];
        final Context.Builder contextBuilder;
        if (language.equals("js")) {
            Map<String, String> options = new HashMap<>();
            options.put("js.commonjs-require", "true");
            options.put("js.commonjs-require-cwd", "/home/vivek/projects/flowScript/src/main/resources");
//            options.put("js.commonjs-core-modules-replacements", "primordials:/home/vivek/projects/flowScript/src/main/resources/node_modules/internal/per_context");
//            options.put("js.commonjs-global-properties", "/home/vivek/projects/flowScript/src/main/resources/node_modules/globals.js");
//            options.put("js.commonjs-core-modules-replacements", "buffer:buffer/");
//            options.put("js.commonjs-core-modules-replacements",
//                    "child_process:/home/vivek/projects/graaljs/graal-nodejs/lib/child_process");
//            options.put("js.commonjs-core-modules-replacements",
//                    "stream:/home/vivek/projects/graaljs/graal-nodejs/lib/stream");
//            options.put("js.commonjs-global-properties", "./globals.js");

            contextBuilder = Context.newBuilder()
                    .allowExperimentalOptions(true)
                    .allowIO(true)
                    .allowAllAccess(true)
                    .options(options);
        } else {
            contextBuilder = Context.newBuilder()
                    .allowAllAccess(true);
        }
        try (Context context = contextBuilder.build()) {

            if(language.equals("js")) {
                context.getBindings("js").putMember("httpRequester", new HttpRequester());
            }

            if (args[1].equals("script")) {
                // js source.js flowId
                context.eval(Source.newBuilder(language, args[2], args[3])
                        .buildLiteral());
            } else {
                context.eval(Source.newBuilder(language, new File(args[1]))
                        .buildLiteral());
            }

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("key", "value");
            Script script = context.getBindings(language)
                    .getMember("mapping")
                    .as(Script.class);

        }
    }




}
