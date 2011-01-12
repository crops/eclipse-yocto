/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *******************************************************************************/
package org.yocto.bc.ui.editors.bitbake;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Rule for def_ BB Recipe functions
 * @author kgilmer
 *
 */
final class CustomFunctionRule implements IRule {

	/** Token to return for this rule */
	private final IToken fToken;

	/**
	 * Creates a new operator rule.
	 * 
	 * @param token
	 *            Token to use for this rule
	 */
	public CustomFunctionRule(IToken token) {
		fToken = token;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		if (scanner.getColumn() > 0) {
			return Token.UNDEFINED;
		}

		int i = scanner.read();
		int c = 1;
		
		if (!Character.isLetter(i) && i != 10) {
			scanner.unread();
			return Token.UNDEFINED;
		}
		
		if (i == 'd' && scanAhead(scanner, "o_".toCharArray())) {
			scanner.unread();
			return Token.UNDEFINED;
		}

		while (i != ICharacterScanner.EOF && i != 10) {
			i = scanner.read();
			c++;
			
			if (i == '(') {
				readUntil(scanner, ')');
				
				return fToken;
			}
		}

		for (int t = 0; t < c; t++) {
			scanner.unread();
		}
		
		return Token.UNDEFINED;
	}

	private void readUntil(ICharacterScanner scanner, int c) {
		int i;
		do {
			i = scanner.read();
		} while (! (i == ICharacterScanner.EOF) && ! (i == c));
	}

	private boolean scanAhead(ICharacterScanner scanner, char [] chars) {
			boolean v = true;
			for (int i = 0; i < chars.length; ++i) {
				if (! (scanner.read() == chars[i])) {
					v = false;
					for (int j = 0; j < i; ++j) {
						scanner.unread();
					}
					break;
				}
			}
			return v;
	}
}