/*******************************************************************************
 * Copyright (c) 2018 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.requestmapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.lsp4j.Range;
import org.springframework.ide.vscode.commons.util.BadLocationException;
import org.springframework.ide.vscode.commons.util.text.TextDocument;

/**
 * @author Martin Lippert
 */
public class WebfluxContentTypeFinder extends ASTVisitor {
	
	private List<WebfluxRouteElement> contentTypes;
	private TextDocument doc;
	
	public WebfluxContentTypeFinder(TextDocument doc) {
		this.doc = doc;
		this.contentTypes = new ArrayList<>();
	}
	
	public List<WebfluxRouteElement> getContentTypes() {
		return contentTypes;
	}
	
	@Override
	public boolean visit(MethodInvocation node) {
		IMethodBinding methodBinding = node.resolveMethodBinding();

		try {
			if (WebfluxUtils.REQUEST_PREDICATES_TYPE.equals(methodBinding.getDeclaringClass().getBinaryName())) {
				String name = methodBinding.getName();
				if (name != null && WebfluxUtils.REQUEST_PREDICATE_CONTENT_TYPE_METHOD.equals(name)) {
					SimpleName nameArgument = WebfluxUtils.extractSimpleNameArgument(node);
					if (nameArgument != null && nameArgument.getFullyQualifiedName() != null) {
						Range range = doc.toRange(nameArgument.getStartPosition(),  nameArgument.getLength());
						contentTypes.add(new WebfluxRouteElement(nameArgument.getFullyQualifiedName().toString(), range));
					}
				}
			}
		}
		catch (BadLocationException e) {
			// ignore
		}

		return !WebfluxUtils.isRouteMethodInvocation(methodBinding);
	}

}
