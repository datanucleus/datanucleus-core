/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    ...
**********************************************************************/
package org.datanucleus.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * Converter for a "matches" regular expression replacing the Java regular expression
 * constructs with datastore-specific constructs.
 */
public class RegularExpressionConverter
{
    private final char zeroOrMoreChar;
    private final char anyChar;
    private final char escapeChar;

    /**
     * Constructor.
     * @param zeroOrMoreChar The pattern string for representing zero or more characters.
     *              Most of databases will use the percent sign character.
     * @param anyChar The pattern string for representing one character.
     *              Most of databases will use the underscore character.
     * @param escapeChar The pattern string for representing to escape zeroOrMoreChar or anyChar.
     *              Most of databases will use the backslash \ character.
     */
    public RegularExpressionConverter(char zeroOrMoreChar, char anyChar, char escapeChar)
    {
        this.zeroOrMoreChar = zeroOrMoreChar;
        this.anyChar = anyChar;
        this.escapeChar = escapeChar;
    }

    /**
     * Convert a regular expression from Java to use the specified constructs.
     * @param input the pattern to parse.
     * @return the converted pattern
     */
    public String convert(String input)
    {
        StringBuilder lit = new StringBuilder();
        char c;

        CharacterIterator ci = new StringCharacterIterator(input);
        while ((c = ci.current()) != CharacterIterator.DONE)
        {
            if (c == '\\') // escape for java match expression
            {
                char ch = ci.next();
                if (ch == CharacterIterator.DONE)
                {
                    lit.append(escapeChar + "\\");
                }
                else if (ch == '.')
                {
                    lit.append(".");
                }
                else if (ch == '\\')
                {
                    // Creating a new String with \\ will swallow the escape \, so we add it back on
                    // Added as a specific case as a reminder that we need to retain the escaping
                    lit.append(escapeChar + "\\" + escapeChar + ch);
                }
                else
                {
                    lit.append(escapeChar + "\\" + ch);
                }
            }
            else if (c == '.')
            {
                int savedIdx = ci.getIndex();
                if (ci.next() == '*')
                {
                    lit.append(zeroOrMoreChar);
                }
                else
                {
                    ci.setIndex(savedIdx);
                    lit.append(anyChar);
                }
            }
            else if (c == anyChar)
            {
                lit.append("" + escapeChar + anyChar);
            }
            else if (c == zeroOrMoreChar)
            {
                lit.append("" + escapeChar + zeroOrMoreChar);
            }
            else
            {
                lit.append(c);
            }
            ci.next();
        }

        return lit.toString();
    }
}