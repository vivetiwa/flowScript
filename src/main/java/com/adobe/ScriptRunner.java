package com.adobe;

import com.adobe.http.HttpRequester;
import com.adobe.util.MapperUtils;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class ScriptRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class.getName());
    //    private static final Pattern BLOB_STORE_URL_PATTERN = Pattern.compile("https://(?<accountName>.*).dfs.core.windows.net");
    private static final Pattern CONTAINER_PATTERN = Pattern.compile("(?<=/)(?<containerName>.*?)(?=/)");

    public static void main(String[] args) {
        System.out.println("Running Script.");
        final String language = args[0];
        try (Context context = Context.newBuilder("js", "python")
                .allowAllAccess(true).build()) {

            context.getBindings(language).putMember("httpRequester", new HttpRequester());
            if (args[1].equals("script")) {
                // js source.js flowId
                context.eval(Source.newBuilder(language, args[2], args[3])
                        .buildLiteral());
            } else if (args[1].equals("git")) {
                context.eval(Source.newBuilder(language, new URL(args[2]))
                        .buildLiteral());
            } else {
                context.eval(Source.newBuilder(language, new File(args[2]))
                        .buildLiteral());
            }

            ScriptRunner scriptRunner = new ScriptRunner();
            JsonNode activityNode = MapperUtils.readTree(scriptRunner.readFile("activity.json"));
            JsonNode linkedServiceNode = MapperUtils.readTree(scriptRunner.readFile("linkedServices.json"));

            if (!linkedServiceNode.isMissingNode()) {
                JsonNode typeProperties = linkedServiceNode.get(0).get("properties").get("typeProperties");
                final String url = typeProperties.get("sasUri").textValue();

                logger.error("Url : {}", url);
                System.out.println("Url : " + url);
                final JsonNode typePropertiesNode = activityNode.get("typeProperties");
                final String folderPath = typePropertiesNode.at("/extendedProperties/folderPath").textValue();
                final String containerName = scriptRunner.containerName(folderPath);

                BlobClient blobClient = new BlobClientBuilder()
                        .endpoint(url)
                        .blobName(folderPath.substring(containerName.length() + 1) + "/" +
                                typePropertiesNode.at("/extendedProperties/filePath").asText())
                        .buildClient();

                Script script = context.getBindings(language)
                        .getMember("mapping")
                        .as(Script.class);

                final byte[] dataRead = blobClient.downloadContent().toBytes();
                if (dataRead.length > 3) {
                    final StringBuilder transformMessage = new StringBuilder();
                    try (MappingIterator<Map<String, Object>> it = MapperUtils.readValues(dataRead)) {
                        while (it.hasNextValue()) {
                            final String transformedRecord = script.mapping(MapperUtils.writeValueAsString(it.next()));
                            final JsonNode transformedNode = MapperUtils.readTree(transformedRecord);
                            if (transformedNode.isArray()) {
                                ArrayNode recordCollection = (ArrayNode) transformedNode;
                                StreamSupport.stream(recordCollection.spliterator(), false)
                                        .forEach(jsonNode -> {
                                            transformMessage.append(MapperUtils.writeValueAsString(jsonNode));
                                            transformMessage.append("\n");
                                        });
                            } else if (transformedNode.isObject()) {
                                transformMessage.append(MapperUtils.writeValueAsString(transformedNode));
                                transformMessage.append("\n");
                            }
                        }
                    }
                    blobClient.upload(BinaryData.fromString(transformMessage.toString()), true);
                    System.out.println("Modified data.");
                } else {
                    System.out.println("File read is empty.");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String containerName(final String filePath) {
        final Matcher accountNameMatcher = CONTAINER_PATTERN.matcher(filePath);
        if (accountNameMatcher.find()) {
            return accountNameMatcher.group("containerName");
        }
        throw new UnsupportedOperationException("containerName not found in url - " + filePath);
    }

    public String readFile(String filename) throws IOException {
        File linkedServices = new File(filename);
        BufferedReader fileReader = new BufferedReader(new FileReader(linkedServices));

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = fileReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }
}
