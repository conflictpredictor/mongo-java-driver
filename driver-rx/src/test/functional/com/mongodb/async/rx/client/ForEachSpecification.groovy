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

package com.mongodb.async.rx.client

import org.mongodb.Document

import static Fixture.get
import static Fixture.getAsList

class ForEachSpecification extends FunctionalSpecification {
    def 'should complete with no results'() {
        expect:
        getAsList(collection.find(new Document()).forEach()) == []
    }

    def 'should call onNext for a document and then complete'() {
        given:
        def document = new Document()
        get(collection.insert(document))

        expect:
        getAsList(collection.find(new Document()).forEach()) == [document]
    }

    def 'should call onNext for each document and then complete'() {
        given:

        def documents = [new Document(), new Document()]
        get(collection.insert(documents[0]))
        sleep(1000)
        get(collection.insert(documents[1]))

        expect:
        getAsList(collection.find(new Document()).forEach()) == documents
    }
}