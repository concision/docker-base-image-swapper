package me.concision.warcrimes.docker.swapper.archive;

import lombok.NonNull;
import me.concision.warcrimes.docker.swapper.archive.DockerImage.Bytes;
import me.concision.warcrimes.docker.swapper.archive.DockerImage.ImageLayer;
import me.concision.warcrimes.docker.swapper.archive.DockerImage.SubImage;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DockerImageIO {
    public static DockerImage read(@NonNull InputStream inputStream) throws IOException {
        // read tar archive
        Map<String, Bytes> files = new LinkedHashMap<>();
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ArchiveInputStream stream = new TarArchiveInputStream(inputStream)) {
                for (ArchiveEntry entry; (entry = stream.getNextEntry()) != null; ) {
                    // reset byte buffer
                    buffer.reset();

                    if (!entry.isDirectory() && stream.canReadEntryData(entry)) {
                        IOUtils.copy(stream, buffer);
                        files.put(entry.getName(), new Bytes(buffer.toByteArray()));
                    }
                }
            }
        }

        // read manifest
        Bytes manifestEntry = files.remove("manifest.json");
        if (manifestEntry == null) throw new RuntimeException("Docker image archive does not have a manifest.json");
        JSONArray manifest = new JSONArray(new String(manifestEntry.bytes(), StandardCharsets.UTF_8));

        // read subimages
        List<SubImage> subimages = new ArrayList<>();
        for (int i = 0; i < manifest.length(); i++) {
            // read manifest
            JSONObject subimageManifest = manifest.getJSONObject(i);

            // read configuration
            String configName = subimageManifest.getString("Config");
            JSONObject config = new JSONObject(new String(files.remove(configName).bytes(), StandardCharsets.ISO_8859_1));

            List<ImageLayer> layers = new ArrayList<>();
            JSONArray layerReferences = subimageManifest.getJSONArray("Layers");
            for (int l = 0; l < layerReferences.length(); l++) {
                // get layer root directory
                String layerArchiveAbsoluteName = layerReferences.getString(l);
                String layerRoot = layerArchiveAbsoluteName.substring(0, layerArchiveAbsoluteName.lastIndexOf('/'));

                // read layer manifest
                JSONObject layerManifest = new JSONObject(new String(files.remove(layerRoot + "/json").bytes(), StandardCharsets.ISO_8859_1));

                // read all other layer files
                Map<String, Bytes> layerFiles = new LinkedHashMap<>();
                for (Iterator<Entry<String, Bytes>> iterator = files.entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, Bytes> fileEntry = iterator.next();
                    if (fileEntry.getKey().startsWith(layerRoot + "/")) {
                        layerFiles.put(fileEntry.getKey().substring(layerRoot.length() + 1), fileEntry.getValue());
                        iterator.remove();
                    }
                }

                layers.add(new ImageLayer(layerRoot, layerManifest, layerFiles));
            }

            subimages.add(new SubImage(
                    configName,
                    config,
                    layers
            ));
        }

        return new DockerImage(
                manifest,
                subimages,
                files
        );
    }

    public static void write(@NonNull DockerImage image, @NonNull OutputStream outputStream) throws IOException {
        try (TarArchiveOutputStream stream = new TarArchiveOutputStream(outputStream)) {
            // write manifest.json
            write(stream, "manifest.json", image.manifest().toString().getBytes(StandardCharsets.UTF_8));

            // write all subimages
            for (SubImage subImage : image.images()) {
                // write subimage configuration
                write(stream, subImage.configurationName(), subImage.configuration().toString().getBytes(StandardCharsets.UTF_8));

                // write all layers
                for (ImageLayer layer : subImage.layers()) {
                    // write layer manifest
                    write(stream, layer.root() + "/json", layer.manifest().toString().getBytes(StandardCharsets.UTF_8));

                    // write layer archive
                    for (Entry<String, Bytes> fileEntry : layer.files().entrySet()) {
                        write(stream, layer.root() + "/" + fileEntry.getKey(), fileEntry.getValue().bytes());
                    }
                }
            }

            // write any remaining files
            for (Entry<String, Bytes> fileEntry : image.files().entrySet()) {
                write(stream, fileEntry.getKey(), fileEntry.getValue().bytes());
            }
        }
    }

    private static void write(TarArchiveOutputStream outputStream, String name, byte[] bytes) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        outputStream.putArchiveEntry(entry);
        outputStream.write(bytes);
        outputStream.closeArchiveEntry();
    }
}
