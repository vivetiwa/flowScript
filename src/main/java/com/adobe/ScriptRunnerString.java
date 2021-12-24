package com.adobe;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptRunnerString {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class.getName());
    //    private static final Pattern BLOB_STORE_URL_PATTERN = Pattern.compile("https://(?<accountName>.*).dfs.core.windows.net");
    private static final Pattern CONTAINER_PATTERN = Pattern.compile("(?<=/)(?<containerName>.*?)(?=/)");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectReader reader = mapper.readerFor(new TypeReference<Map<String, Object>>() {});

    public static void main(String[] args) {
        System.out.println("Running Script.");
        final String language = args[0];
        try (Context context = Context.newBuilder("js", "python")
//                .option("python.ForceImportSite", "true")
                .allowAllAccess(true).build()) {

            if(args[1].equals("script")) {
                // js source.js flowId
                context.eval(Source.newBuilder(language, args[2], args[3])
                        .buildLiteral());
            } else {
                context.eval(Source.newBuilder(language, new File(args[1]))
                        .buildLiteral());
            }

            ScriptRunnerString scriptRunner = new ScriptRunnerString();
            JsonNode activityNode = mapper.readTree(scriptRunner.readFile("activity.json"));
            JsonNode linkedServiceNode = mapper.readTree(scriptRunner.readFile("linkedServices.json"));

            if(Objects.nonNull(linkedServiceNode)) {
                JsonNode typeProperties = linkedServiceNode.get(0).get("properties").get("typeProperties");
                final String url = typeProperties.get("sasUri").textValue();

                logger.error("Url : {}", url);
                System.out.println("Url : " + url);
                final JsonNode typePropertiesNode = activityNode.get("typeProperties");
                final String folderPath = typePropertiesNode.at("/extendedProperties/folderPath").textValue();
                final String containerName = scriptRunner.containerName(folderPath);

                BlobClient blobClient = new BlobClientBuilder()
                        .endpoint(url)
                        .blobName(folderPath.substring(containerName.length()+1) + "/" +
                                typePropertiesNode.at("/extendedProperties/filePath").asText())
                        .buildClient();
//
                ScriptString script = context.getBindings(language)
                        .getMember("mapping")
                        .as(ScriptString.class);

//                  final byte[] dataRead = "{\"name\": \"abc\"}".getBytes();
                final byte[] dataRead = blobClient.downloadContent().toBytes();
//                if(dataRead.length > 3) {
                    final StringBuilder transformMessage = new StringBuilder();
                    try (MappingIterator<Map<String, Object>> it = reader.readValues(dataRead)) {
                        while (it.hasNextValue()) {
                            transformMessage.append(script.mapping(mapper.writeValueAsString(it.next())));
                            transformMessage.append("\n");
                        }
                    }
//                    blobClient.upload(BinaryData.fromString(transformMessage.toString()), true);
                    System.out.println("Modified data.");
//                } else {
//                    System.out.println("File read is empty.");
//                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public String accountName(final String url) {
//       final Matcher accountNameMatcher = BLOB_STORE_URL_PATTERN.matcher(url);
//       if(accountNameMatcher.find()) {
//           return accountNameMatcher.group("accountName");
//       }
//       throw new UnsupportedOperationException("accountName not found in url - " + url);
//    }

    public String containerName(final String filePath) {
        final Matcher accountNameMatcher = CONTAINER_PATTERN.matcher(filePath);
        if(accountNameMatcher.find()) {
            return accountNameMatcher.group("containerName");
        }
        throw new UnsupportedOperationException("containerName not found in url - " + filePath);
    }

    public String readFile(String filename) throws IOException {
        File linkedServices = new File(filename);
        BufferedReader fileReader = new BufferedReader(new FileReader(linkedServices));

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while((line = fileReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }
}
