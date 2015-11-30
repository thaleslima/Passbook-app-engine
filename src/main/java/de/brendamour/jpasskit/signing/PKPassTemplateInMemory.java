/**
 * Copyright (C) 2015 Patrice Brend'amour <patrice@brendamour.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.brendamour.jpasskit.signing;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class PKPassTemplateInMemory implements IPKPassTemplate {

    public static final String PK_ICON_RETINA = "icon@2x.png";
    public static final String PK_ICON = "icon.png";
    public static final String PK_LOGO_RETINA = "logo@2x.png";
    public static final String PK_LOGO = "logo.png";
    public static final String PK_THUMBNAIL_RETINA = "thumbnail@2x.png";
    public static final String PK_THUMBNAIL = "thumbnail.png";
    public static final String PK_STRIP_RETINA = "strip@2x.png";
    public static final String PK_STRIP = "strip.png";
    public static final String PK_BACKGROUND_RETINA = "background@2x.png";
    public static final String PK_BACKGROUND = "background.png";

    private Map<String, InputStream> files = new ConcurrentHashMap<>();

    @Override
    public void provisionPassAtDirectory(File tempPassDir) throws IOException {
        for (Entry<String, InputStream> entry : files.entrySet()) {
            InputStream stream = entry.getValue();
            File pathToFile = new File(tempPassDir, entry.getKey());
            pathToFile.getParentFile().mkdir();
            //FileUtils.copyInputStreamToFile(stream, pathToFile);
            IOUtils.closeQuietly(stream);
        }
    }

    @Override
    public Map<String, ByteBuffer> getAllFiles() throws IOException {
        Map<String, ByteBuffer> allFiles = new HashMap<>();
        for (Entry<String, InputStream> entry : files.entrySet()) {
            byte[] byteArray = IOUtils.toByteArray(entry.getValue());
            String filePath = entry.getKey();
            allFiles.put(filePath, ByteBuffer.wrap(byteArray));
        }
        return allFiles;
    }

    public void addFile(String pathInTemplate, File file) throws IOException {
        files.put(pathInTemplate, new FileInputStream(file));
    }

    public void addFile(String pathInTemplate, InputStream stream) throws IOException {
        files.put(pathInTemplate, stream);
    }

    public void addFile(String pathInTemplate, StringBuffer content) throws IOException {
        files.put(pathInTemplate, new ByteArrayInputStream(content.toString().getBytes()));
    }

    public void addFile(String pathInTemplate, URL contentURL) throws IOException {
        files.put(pathInTemplate, contentURL.openStream());
    }

    public void addFile(String pathInTemplate, Locale locale, File file) throws IOException {
        addFile(pathForLocale(pathInTemplate, locale), new FileInputStream(file));
    }

    public void addFile(String pathInTemplate, Locale locale, InputStream stream) throws IOException {
        addFile(pathForLocale(pathInTemplate, locale), stream);
    }

    public void addFile(String pathInTemplate, Locale locale, StringBuffer content) throws IOException {
        addFile(pathForLocale(pathInTemplate, locale), new ByteArrayInputStream(content.toString().getBytes()));
    }

    public void addFile(String pathInTemplate, Locale locale, URL contentURL) throws IOException {
        addFile(pathForLocale(pathInTemplate, locale), contentURL.openStream());
    }

    public Map<String, InputStream> getFiles() {
        return files;
    }

    private String pathForLocale(String pathInTemplate, Locale locale) {
        if (locale == null) {
            return pathInTemplate;
        }

        return locale.getLanguage() + ".lproj" + File.separator + pathInTemplate;
    }

}
