// Copyright 2010 Atos Worldline
//
// Inspired from Backelite bkxititag library for iPhone
// Copyright 2009 Backelite
// see http://code.google.com/p/bkxititag/
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.awl.android.xiti;

/**
 * Operation to send Xiti stats.
 * 
 * @author Cyril Cauchois
 */
public class XitiTagOperation {
	
	private String operation;
	private String subsiteId;
	
	public XitiTagOperation(String operation) {
		this(operation, null);
	}
	
	public XitiTagOperation(String operation, String subsiteId) {
		this.operation = operation;
		this.subsiteId = subsiteId;
	}
	
	public String getOperation() {
		return operation;
	}

	public String getSubsiteId() {
		return subsiteId;
	}

}
