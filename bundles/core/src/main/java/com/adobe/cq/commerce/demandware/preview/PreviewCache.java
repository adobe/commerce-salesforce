/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.adobe.cq.commerce.demandware.preview;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple cache for caching of rendered content fragments.
 */
public class PreviewCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewCache.class);
    private int size = 1000;
    private int age = 60;
    private LinkedHashMap<Key, String> resourceMap = new LinkedHashMap<Key, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, String> eldest) {
            return size < size();
        }
    };

    public PreviewCache(int age) {
        this.age = age;
    }

    public synchronized String get(Resource resource) {
        if (resource == null)
            return null;

        clean();
        return resourceMap.get(Key.forAccess(resource));
    }

    public synchronized void put(Resource resource, String renderedPreview) {
        if (resource == null || renderedPreview == null)
            return;

        clean();
        resourceMap.put(Key.forStore(resource), renderedPreview);
    }

    private void clean() {
        int size = resourceMap.size();
        final long time = System.currentTimeMillis() - age * 1000;
        for (Iterator<Key> iterator = resourceMap.keySet().iterator(); iterator.hasNext(); ) {
            Key key = iterator.next();
            if (key.older(time)) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    private static class Key {
        final long time;
        final String path;

        private Key(String path, long time) {
            this.time = time;
            this.path = path;
        }

        private static Key forStore(Resource resource) {
            return new Key(resource.getPath(), System.currentTimeMillis());
        }

        private static Key forAccess(Resource resource) {
            return new Key(resource.getPath(), 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!path.equals(key.path)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        public boolean older(long val) {
            return time < val;
        }

        @Override
        public String toString() {
            return "Key{time=" + time + ", path='" + path + "'}";
        }
    }
}
