/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.ytex.ws;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "simServiceInfo")
public class SimServiceInfo {
	String conceptGraph;
	String description;
	public String getConceptGraph() {
		return conceptGraph;
	}
	public void setConceptGraph(String conceptGraph) {
		this.conceptGraph = conceptGraph;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public SimServiceInfo() {
		super();
	}
	public SimServiceInfo(String conceptGraph, String description) {
		super();
		this.conceptGraph = conceptGraph;
		this.description = description;
	}

}
