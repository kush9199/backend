package dev.monorepo.shared.responseHandler.config;

import dev.monorepo.shared.responseHandler.common.ErrorPayload;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YamlErrorLoader {
    public static Map<String, ErrorPayload> load(String path, ResourceLoader loader) throws IOException {
        if(path == null) return Map.of();
        try{
            var resource = loader.getResource(path);
            if(!resource.exists()) return Map.of();
            try(var in = resource.getInputStream()){
                var yaml = new Yaml();
                Map<String, Object> root = yaml.load(in);
                var rawErrors = (Map<String, Map<String, Object>>) root.get("errors");
                Map<String, ErrorPayload> result = new HashMap<>();
                for(var entry: rawErrors.entrySet()){
                    var v = entry.getValue();

                    if(((String) v.get("message")).isEmpty() ||
                            v.get("httpStatus").equals(null) ||
                            ((String)v.get("category")).isEmpty()){
                        throw new IllegalStateException("malformed errors in config");
                    }
                    result.put(entry.getKey(), new ErrorPayload(
                            entry.getKey(),
                            (String) v.get("message"),
                            (int) v.get("httpStatus"),
                            (String) v.get("category")
                    ));
                    return result;
                }
            }
        }catch (Exception ex){
            throw new IllegalStateException("Failed to load error config: " + path, ex);
        }
        return Map.of();
    }
}
