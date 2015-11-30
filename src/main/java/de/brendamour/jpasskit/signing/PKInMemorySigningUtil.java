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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import de.brendamour.jpasskit.PKPass;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.inject.Inject;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class PKInMemorySigningUtil extends PKAbstractSIgningUtil {

    private static final String MANIFEST_JSON_FILE_NAME = "manifest.json";
    private static final String PASS_JSON_FILE_NAME = "pass.json";
    private static final String SIGNATURE_FILE_NAME = "signature";

    private ObjectWriter objectWriter;

    @Inject
    public PKInMemorySigningUtil(ObjectMapper objectMapper) {
        addBCProvider();
        objectWriter = configureObjectMapper(objectMapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.brendamour.jpasskit.signing.IPKSigningUtil#createSignedAndZippedPkPassArchive(de.brendamour.jpasskit.PKPass,
     * de.brendamour.jpasskit.signing.IPKPassTemplate, de.brendamour.jpasskit.signing.PKSigningInformation)
     */

    @Override
    public byte[] createSignedAndZippedPkPassArchive(String pass, IPKPassTemplate passTemplate, PKSigningInformation signingInformation)
            throws PKSigningException {
        Map<String, ByteBuffer> allFiles;
         try {
         allFiles = passTemplate.getAllFiles();
         } catch (IOException e) {
         throw new PKSigningException("Error when getting files from template", e);
         }

        allFiles.put(PASS_JSON_FILE_NAME, createPassJSONFile(pass));

        ByteBuffer manifestJSONFile = createManifestJSONFile(allFiles);
        allFiles.put(MANIFEST_JSON_FILE_NAME, manifestJSONFile);

        ByteBuffer signature = ByteBuffer.wrap(signManifestFile(manifestJSONFile.array(), signingInformation));
        allFiles.put(SIGNATURE_FILE_NAME, signature);

        return createZippedPassAndReturnAsByteArray(allFiles);
    }

/*    private ByteBuffer createPassJSONFile(final PKPass pass) throws PKSigningException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            objectWriter.writeValue(byteArrayOutputStream, pass);
            return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            throw new PKSigningException("Error when writing pass.json", e);
        } finally {
            IOUtils.closeQuietly(byteArrayOutputStream);
        }
    }*/

    public ByteBuffer createPassJSONFile(final String pass) throws PKSigningException
    {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try {
            OutputStream oos = new BufferedOutputStream(bytesOut);

            oos.write(pass.getBytes());
            oos.flush();

            byte[] bytes = bytesOut.toByteArray();
            bytesOut.close();
            oos.close();

            return ByteBuffer.wrap(bytes);
        } catch (Exception e) {
            throw new PKSigningException("Error when writing pass.json", e);
        } finally {
            IOUtils.closeQuietly(bytesOut);
        }
    }


    private ByteBuffer createManifestJSONFile(Map<String, ByteBuffer> allFiles) throws PKSigningException {
        Map<String, String> fileWithHashMap = new HashMap<String, String>();

        HashFunction hashFunction = Hashing.sha1();
        hashFiles(allFiles, fileWithHashMap, hashFunction);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            objectWriter.writeValue(byteArrayOutputStream, fileWithHashMap);
            return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new PKSigningException("Error when writing pass.json", e);
        } finally {
            IOUtils.closeQuietly(byteArrayOutputStream);
        }
    }

    private void hashFiles(Map<String, ByteBuffer> files, final Map<String, String> fileWithHashMap, final HashFunction hashFunction)
            throws PKSigningException {
        for (Entry<String, ByteBuffer> passResourceFile : files.entrySet()) {
            HashCode hash = hashFunction.hashBytes(passResourceFile.getValue().array());
            fileWithHashMap.put(passResourceFile.getKey(), Hex.encodeHexString(hash.asBytes()));
        }
    }

    private byte[] createZippedPassAndReturnAsByteArray(final Map<String, ByteBuffer> files) throws PKSigningException {
        ByteArrayOutputStream byteArrayOutputStreamForZippedPass = null;
        ZipOutputStream zipOutputStream = null;
        byteArrayOutputStreamForZippedPass = new ByteArrayOutputStream();
        zipOutputStream = new ZipOutputStream(byteArrayOutputStreamForZippedPass);
        for (Entry<String, ByteBuffer> passResourceFile : files.entrySet()) {
            ZipEntry entry = new ZipEntry(getRelativePathOfZipEntry(passResourceFile.getKey(), ""));
            try {
                zipOutputStream.putNextEntry(entry);
                IOUtils.copy(new ByteArrayInputStream(passResourceFile.getValue().array()), zipOutputStream);
            } catch (IOException e) {
                IOUtils.closeQuietly(zipOutputStream);
                throw new PKSigningException("Error when zipping file", e);
            }
        }
        IOUtils.closeQuietly(zipOutputStream);
        return byteArrayOutputStreamForZippedPass.toByteArray();
    }

    private void addBCProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

    }
}
