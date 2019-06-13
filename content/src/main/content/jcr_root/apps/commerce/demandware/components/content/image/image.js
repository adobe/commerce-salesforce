/*******************************************************************************
 * Copyright 2018 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*global use, resource, log, wcmmode, request, properties */
use([
        '/libs/wcm/foundation/components/utils/ResourceUtils.js',
        '/libs/wcm/foundation/components/utils/AuthoringUtils.js'
    ],
    function (ResourceUtils, AuthoringUtils) {
        'use strict';

        const PROP_FILE_REFERENCE = 'fileReference',
            PROP_ALT = 'alt',
            PROP_IS_DECORATIVE = 'isDecorative',
            PROP_WIDTH = 'width',
            PROP_HEIGHT = 'height',
            PROP_LINK_URL = 'linkURL',
            PROP_IMAGE_MAP = 'imageMap',

            RENDITIONS_PATH = 'renditions',
            WEB_RENDITION_PREFIX = 'cq5dam.web.',
            FILE_CHILD_NODE = 'file',

            AUTHOR_IMAGE_CLASS = 'cq-dd-image',
            PLACEHOLDER_SRC = '/etc/designs/default/0.gif',
            PLACEHOLDER_TOUCH_CLASS = 'cq-placeholder file',
            PLACEHOLDER_CLASSIC_CLASS = 'cq-image-placeholder';

        var resolver = resource.getResourceResolver(),
            timestamp = new Date().getTime(),
            image = {};

        image.fileReference = function () {
            return properties.get(PROP_FILE_REFERENCE);
        };

        image.fileName = function () {
            var filePath = this.fileReference() || resource.path,
                lastIndex = filePath.lastIndexOf('/');
            return lastIndex >= 0 ? filePath.substring(lastIndex + 1) : filePath;
        };

        image.src = function () {
            var fileReference = this.fileReference(),
                reference,
                jcrContent,
                renditionsFolder,
                index,
                renditions = [],
                rendition,
                file;
            if (fileReference) {
                reference = resolver.getResource(fileReference);
                if (reference) {
                    jcrContent = reference.getChild('jcr:content');
                    if (jcrContent) {
                        renditionsFolder = jcrContent.getChild(RENDITIONS_PATH);
                        if (renditionsFolder) {
                            renditions = renditionsFolder.getChildren();
                            for (index = 0; index < renditions.length; index++) {
                                rendition = renditions[index];
                                if (rendition.name.indexOf(WEB_RENDITION_PREFIX) !== -1) {
                                    log.debug('Found web rendition ' + rendition.path);
                                    return _appendCacheKillerIfNeeded(
                                        request.getContextPath() + rendition.path, _getImageLastModifiedDate(rendition)
                                    );
                                }
                            }
                        }
                        log.debug('Could not find any web renditions for ' + fileReference);
                        return _appendCacheKillerIfNeeded(request.getContextPath() + fileReference, _getImageLastModifiedDate(reference));
                    }
                }
            } else {
                file = resource.getChild(FILE_CHILD_NODE);
                if (file) {
                    log.debug('Found file sub-node ' + file.path);
                    return _appendCacheKillerIfNeeded(request.getContextPath() + file.path, _getImageLastModifiedDate(file));
                }
            }
            return request.getContextPath() + PLACEHOLDER_SRC;
        };

        image.imageMaps = function () {
            var imgMaps,
                mapInfo = properties.get(PROP_IMAGE_MAP);

            if (mapInfo) {
                var name = 'map-' + timestamp;
                imgMaps = {
                    name: name,
                    hash: '#' + name,
                    maps: []
                };

                var mapInfoRegex = /\[([^(]*)\(([^)]*)\)([^|]*)\|([^|]*)\|([^\]]*)\]/g;
                var match;
                while ((match = mapInfoRegex.exec(mapInfo))) {
                    imgMaps.maps.push({
                        type: match[1],
                        coords: match[2],
                        href: match[3].replace(/"([^"]*)"/g, '$1'),
                        target: match[4].replace(/"([^"]*)"/g, '$1'),
                        text: match[5].replace(/"([^"]*)"/g, '$1')
                    });
                }
            }
            return imgMaps;
        };

        image.alt = function () {
            if (this.isDecorative()) {
                // declared decorative image: null alt attribute
                return true;
            } else {
                // use alt or remove the alt attribute completely (latter allows accessibility checking tools to
                // report missing alt attributes)
                return properties.get(PROP_ALT) || false;
            }
        };

        image.isDecorative = function () {
            return properties.get(PROP_IS_DECORATIVE);
        };

        image.title = function () {
            return properties.get('jcr:title');
        };

        image.width = function () {
            return properties.get(PROP_WIDTH);
        };

        image.height = function () {
            return properties.get(PROP_HEIGHT);
        };

        image.linkUrl = function () {
            return properties.get(PROP_LINK_URL);
        };

        function _getImageLastModifiedDate(resource) {
            var jcrContent,
                lastModified = resource.getResourceMetadata().getCreationTime(),
                tmp;
            jcrContent = resource.getChild('jcr:content');
            if (jcrContent) {
                tmp = jcrContent.getResourceMetadata().getModificationTime();
                if (tmp > lastModified) {
                    lastModified = tmp;
                }
            }
            return lastModified;
        }

        function _appendCacheKillerIfNeeded(path, cacheKillerValue) {
            if (wcmmode.isEdit()) {
                return path + '?' + cacheKillerValue;
            }
            return path;
        }

        var imageLocalFile = resource.getChild('file');
        if (!imageLocalFile) {
            image.cssClass = AUTHOR_IMAGE_CLASS;

            if (!image.fileReference()) {
                image.src = PLACEHOLDER_SRC;
                if (AuthoringUtils.isTouch) {
                    image.cssClass = image.cssClass + ' ' + PLACEHOLDER_TOUCH_CLASS;
                } else if (AuthoringUtils.isClassic) {
                    image.cssClass = image.cssClass + ' ' + PLACEHOLDER_CLASSIC_CLASS;
                }
            }

        }

        if (image.width() <= 0) {
            image.width = '';
        }
        if (image.height() <= 0) {
            image.height = '';
        }

        return image;
    }
);
