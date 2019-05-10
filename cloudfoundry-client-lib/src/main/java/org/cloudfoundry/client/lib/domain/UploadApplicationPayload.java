/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.lib.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.io.DynamicZipInputStream;
import org.cloudfoundry.client.lib.io.DynamicZipInputStream.Entry;

/**
 * A payload used to upload application data. The payload data is built from a source {@link ApplicationArchive}, excluding any entries that
 * are already known to the remote server.
 *
 * @author Phillip Webb
 */
public class UploadApplicationPayload {

    private ApplicationArchive archive;

    private ArrayList<Entry> entriesToUpload;

    private int totalUncompressedSize;

    /**
     * Create a new {@link UploadApplicationPayload}.
     *
     * @param archive the source archive
     */
    public UploadApplicationPayload(ApplicationArchive archive) {
        this.archive = archive;
        this.totalUncompressedSize = 0;
        this.entriesToUpload = new ArrayList<>();
        for (ApplicationArchive.Entry entry : archive.getEntries()) {
            entriesToUpload.add(new DynamicZipInputStreamEntryAdapter(entry));
            totalUncompressedSize += entry.getSize();
        }
    }

    /**
     * Returns the source archive.
     *
     * @return the archive
     */
    public ApplicationArchive getArchive() {
        return archive;
    }

    /**
     * Returns the payload data as an input stream.
     *
     * @return the payload data
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(archive.getPath());
    }

    /**
     * @return The total number of entries to upload
     */
    public int getNumEntries() {
        return entriesToUpload.size();
    }

    /**
     * Returns the total size of the entries to be transfered (before compression).
     *
     * @return the uncompressed size of the entries.
     */
    public int getTotalUncompressedSize() {
        return totalUncompressedSize;
    }

    /**
     * Internal adapter used to convert {@link ApplicationArchive.Entry} into {@link DynamicZipInputStream.Entry}.
     */
    private static class DynamicZipInputStreamEntryAdapter implements DynamicZipInputStream.Entry {

        private ApplicationArchive.Entry entry;

        public DynamicZipInputStreamEntryAdapter(ApplicationArchive.Entry entry) {
            this.entry = entry;
        }

        public InputStream getInputStream() throws IOException {
            return entry.getInputStream();
        }

        public String getName() {
            return entry.getName();
        }
    }
}
