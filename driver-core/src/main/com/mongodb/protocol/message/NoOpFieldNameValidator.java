/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.protocol.message;

import org.bson.FieldNameValidator;

/**
 * A field name validator that treats all fields as valid.
 *
 * @since 3.0
 */
public class NoOpFieldNameValidator implements FieldNameValidator {
    @Override
    public boolean validate(final String fieldName) {
        return true;
    }

    @Override
    public FieldNameValidator getValidatorForField(final String fieldName) {
        return this;
    }
}
