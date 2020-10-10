package me.concision.warcrimes.docker.swapper.transformers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.api.DockerContainerConfig;
import me.concision.warcrimes.docker.swapper.api.DockerImageConfig;
import me.concision.warcrimes.docker.swapper.api.DockerLayerManifest;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
public class T06ReconcileConfiguration implements ImageTransformer {
    @Override
    public void transform(ImageState state) {
        // reconcile config/container_config of old.lastlayer, new.lastlayer, input.manifest, out.lastLayer
        {
            DockerLayerManifest old = state.oldImage().layers().get(state.oldImage().layers().size() - 1).manifest();
            DockerLayerManifest new_ = state.newImage().layers().get(state.newImage().layers().size() - 1).manifest();
            DockerLayerManifest input = state.inputImage().layers().get(state.inputImage().layers().size() - 1).manifest();
            DockerLayerManifest out = state.outImage().layers().get(state.outImage().layers().size() - 1).manifest();

            if (old.config() != null && new_.config() != null && input.config() != null)
                reconcile(old.config(), new_.config(), input.config(), out.config());
            reconcile(old.containerConfig(), new_.containerConfig(), input.containerConfig(), out.containerConfig());
        }

        // reconcile config/container_config of old.manifest, new.manifest, input.manifest ,out.manfiest
        {

            DockerImageConfig old = state.oldImage().config();
            DockerImageConfig new_ = state.newImage().config();
            DockerImageConfig input = state.inputImage().config();
            DockerImageConfig out = state.outImage().config();

            reconcile(old.config(), new_.config(), input.config(), out.config());
            reconcile(old.containerConfig(), new_.containerConfig(), input.containerConfig(), out.containerConfig());
        }
    }

    private static void reconcile(DockerContainerConfig oldConfig, DockerContainerConfig newConfig, DockerContainerConfig inputConfig, DockerContainerConfig outConfig) {
        JsonObject oldJson = oldConfig.json();
        JsonObject newJson = newConfig.json();
        JsonObject inputJson = inputConfig.json();
        JsonObject outJson = outConfig.json();

        for (String key : new String[]{"Domainname", "User", "Cmd", "WorkingDir", "Entrypoint", "OnBuild"}) {
            if (Objects.equals(oldJson.get(key), inputJson.get(key))) {
                outJson.add(key, newJson.get(key));
            }
        }

        for (String key : new String[]{"ExposedPorts", "Volumes", "Labels"}) {
            if (inputJson.get(key) != null && !inputJson.get(key).isJsonNull()) {
                JsonObject inputPorts = inputJson.getAsJsonObject(key);
                if (oldJson.get(key) != null && !oldJson.get(key).isJsonNull()) {
                    JsonObject oldPorts = oldJson.getAsJsonObject(key);
                    List<String> remove = new ArrayList<>();
                    for (String s : inputPorts.keySet()) {
                        if (Objects.equals(inputPorts.get(s), oldPorts.get(s))) {
                            remove.add(s);
                        }
                    }
                    for (String s : remove) {
                        inputPorts.remove(s);
                    }
                }

                if (newJson.get(key) != null && !newJson.get(key).isJsonNull()) {
                    JsonObject newPorts = newJson.getAsJsonObject(key);
                    for (String s : newPorts.keySet()) {
                        inputPorts.add(s, newPorts.get(s));
                    }
                }
                outJson.add(key, inputPorts);
            } else {
                outJson.add(key, newJson.get(key));
            }
        }

        if (inputJson.get("Env") != null && !inputJson.get("Env").isJsonNull()) {
            JsonArray inputEnv = inputJson.getAsJsonArray("Env");
            JsonArray outEnv = new JsonArray();

            boolean isPathHandled = false;
            JsonArray oldEnv = oldJson.get("Env") != null && !oldJson.get("Env").isJsonNull() ? oldJson.getAsJsonArray("Env") : null;
            if (oldEnv != null) {
                for (int i = 0; i < inputEnv.size(); i++) {
                    String env = inputEnv.get(i).getAsString();
                    if (!oldEnv.contains(new JsonPrimitive(env))) {
                        outEnv.add(env);
                    }
                }
            } else {
                for (int i = 0; i < inputEnv.size(); i++) {
                    outEnv.add(inputEnv.get(i).getAsString());
                }
            }
            JsonArray newEnv = newJson.get("Env") != null && !newJson.get("Env").isJsonNull() ? newJson.getAsJsonArray("Env") : null;
            if (newEnv != null) {
                outer:
                for (int i = 0; i < newEnv.size(); i++) {
                    String env = newEnv.get(i).getAsString();

                    if (!outEnv.contains(new JsonPrimitive(env))) {
                        if (0 <= env.indexOf('=')) {
                            String name = env.substring(0, env.indexOf('='));

                            for (int j = 0; j < outEnv.size(); j++) {
                                String outEnvVar = outEnv.get(j).getAsString();
                                if (outEnvVar.startsWith(name + "=")) {
                                    continue outer;
                                }
                            }
                        }

                        outEnv.add(env);
                    }
                }
            }

            // reconcile special PATH
            if (oldEnv != null && newEnv != null) {
                String oldPath = null;
                for (int i = 0; i < oldEnv.size(); i++) {
                    String asString = oldEnv.get(i).getAsString();
                    if (asString.startsWith("PATH=")) {
                        oldPath = asString.substring("PATH=".length());
                        break;
                    }
                }

                String newPath = null;
                for (int i = 0; i < newEnv.size(); i++) {
                    String asString = newEnv.get(i).getAsString();
                    if (asString.startsWith("PATH=")) {
                        newPath = asString.substring("PATH=".length());
                        break;
                    }
                }

                if (oldPath != null && newPath != null)
                    for (int i = 0; i < outEnv.size(); i++) {
                        String env = outEnv.get(i).getAsString();
                        if (env.startsWith("PATH=")) {
                            outEnv.set(i, new JsonPrimitive(env.replace(oldPath, newPath)));
                        }
                    }
            }
            outJson.add("Env", outEnv);
        } else {
            outJson.add("Env", newJson.get("Env"));
        }
    }
}
