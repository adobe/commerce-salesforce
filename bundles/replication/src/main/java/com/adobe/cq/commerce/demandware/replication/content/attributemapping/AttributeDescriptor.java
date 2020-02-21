/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated and others
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

package com.adobe.cq.commerce.demandware.replication.content.attributemapping;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

/**
 * Holds values of attribute mapping so target name/converter id do not have
 * to be joined and split over and over again.
 */
public class AttributeDescriptor {

    private final String targetName;
    private final String converterId;
    private final String sourceName;
    private final String defaultValue;

    public AttributeDescriptor(final String sourceName, final String targetName) {
        this(sourceName, targetName, null);
    }

    public AttributeDescriptor(final String sourceName, final String targetName, final String converterId) {
        this(sourceName, targetName, converterId, null);
    }

    public AttributeDescriptor(final String sourceName, final String targetName, final String converterId, final String defaultValue) {
        if(isBlank(sourceName)) {
            throw new IllegalArgumentException("sourceName must not be blank.");
        }

        if(isBlank(targetName)) {
            throw new IllegalArgumentException("targetName must not be blank.");
        }

        this.sourceName = sourceName;
        this.targetName = targetName;
        this.converterId = converterId;
        this.defaultValue = defaultValue;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getConverterId() {
        return converterId;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override // Generated. Don't edit.
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AttributeDescriptor)) return false;

        AttributeDescriptor that = (AttributeDescriptor) o;

        return new EqualsBuilder()
                .append(targetName, that.targetName)
                .append(sourceName, that.sourceName)
                .isEquals();
    }

    @Override // Generated. Don't edit.
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(targetName)
                .append(sourceName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("AttributeDescriptor[")
                .append("sourceName:").append(sourceName)
                .append(",targetName:").append(targetName)
                .append(",converterId:").append(converterId)
                .append(",defaultValue:").append(defaultValue)
                .append(']')
                .toString();
    }
}
