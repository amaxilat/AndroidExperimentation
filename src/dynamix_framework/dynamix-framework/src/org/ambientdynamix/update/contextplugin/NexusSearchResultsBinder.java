/*
 * Copyright (C) The Ambient Dynamix Project
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
package org.ambientdynamix.update.contextplugin;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.List;

/**
 * Simple Framework binder class for XML parsing.
 *
 * @author Darren Carlson
 */
@Element(name = "search-results")
class NexusSearchResultsBinder {
    @Element
    int totalCount;
    @Element
    int from;
    @Element
    int count;
    @Element
    boolean tooManyResults;

    @ElementList(required = false)
    public List<NexusArtifactBinder> data;

}