/**********************************************************************
Copyright (c) 2002 Kelly Grizzle (TJDO) and others. All rights reserved. 
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
2003 Andy Jefferson - coding standards
2005 Andy Jefferson - added support for single quoted StringLiteral (incl. contrib. from Tony Lai)
2008 Erik Bengtson - adapted to generic Lexer
2008 Andy Jefferson - add parameter prefixes as input to allow JPQL use.
2008 Andy Jefferson - add parseCast
    ...
**********************************************************************/
package org.datanucleus.query.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;

/**
 * Lexer for a Query.
 * Allows a class to work its way through the parsed string, obtaining relevant components with each 
 * call, or peeking ahead before deciding what component to parse next.
 * Would work with JDOQL or JPQL. The only difference is the input of parameter prefixes.
 * With JDOQL all parameters are prefixed ":", whereas in JPQL you can have numbered parameters "?"
 * and named parameters ":".
 */
public class Lexer
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Prefixes for any parameter, in a string. JDOQL will be ":". JPQL will be ":?". */
    private final String parameterPrefixes;

    private final String input;

    protected final CharacterIterator ci;

    private final boolean parseEscapedChars;

    /**
     * Constructor
     * @param input The input string
     * @param paramPrefixes String containing all possible prefixes for parameters
     * @param parseEscapedChars Whether to prase escaped characters
     */
    public Lexer(String input, String paramPrefixes, boolean parseEscapedChars)
    {
        this.input = input;
        this.parameterPrefixes = paramPrefixes;
        this.parseEscapedChars = parseEscapedChars;

        ci = new StringCharacterIterator(input);
    }

    /**
     * Accessor for the input string.
     * @return The input string.
     */
    public String getInput()
    {
        return input;
    }

    /**
     * Accessor for the current index in the input string.
     * @return The current index.
     */
    public int getIndex()
    {
        return ci.getIndex();
    }

    /**
     * Skip over any whitespace from the current position.
     * @return The new position
     */
    public int skipWS()
    {
        int startIdx = ci.getIndex();
        char c = ci.current();

        while (Character.isWhitespace(c) || 
               c == '\t' || c == '\f' ||
               c == '\n' || c == '\r' ||
               c == '\u0009' || c == '\u000c' ||
               c == '\u0020' || c == '\11' ||
               c == '\12' || c == '\14' ||
               c == '\15' || c == '\40')
        {
            c = ci.next();
        }

        return startIdx;
    }

    /**
     * Check if END OF TEXT is reached.
     * @return true if END OF TEXT is reached
     */
    public boolean parseEOS()
    {
        skipWS();

        return ci.current() == CharacterIterator.DONE;
    }

    /**
     * Check if char <code>c</code> is found
     * @param c the Character to find
     * @return true if <code>c</code> is found
     */
    public boolean parseChar(char c)
    {
        skipWS();

        if (ci.current() == c)
        {
            ci.next();
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Check if char <code>c</code> is found
     * @param c the Character to find
     * @param unlessFollowedBy the character to validate it does not follow <code>c</code>
     * @return true if <code>c</code> is found and not followed by <code>unlessFollowedBy</code>
     */
    public boolean parseChar(char c, char unlessFollowedBy)
    {
        int savedIdx = skipWS();

        if (ci.current() == c && ci.next() != unlessFollowedBy)
        {
            return true;
        }
        else
        {
            ci.setIndex(savedIdx);
            return false;
        }
    }

    /**
     * Check if String <code>s</code> is found
     * @param s the String to find
     * @return true if <code>s</code> is found
     */
    public boolean parseString(String s)
    {
        int savedIdx = skipWS();

        int len = s.length();
        char c = ci.current(); 

        for (int i = 0; i < len; ++i)
        {
            if (c != s.charAt(i))
            {
                ci.setIndex(savedIdx);
                return false;
            }

            c = ci.next();
        }

        return true;
    }

    /**
     * Check if String <code>s</code> is found ignoring the case
     * @param s the String to find
     * @return true if <code>s</code> is found
     */
    public boolean parseStringIgnoreCase(String s)
    {
        String lowerCasedString = s.toLowerCase();
        
        int savedIdx = skipWS();

        int len = lowerCasedString.length();
        char c = ci.current(); 

        for (int i = 0; i < len; ++i)
        {
            if (Character.toLowerCase(c) != lowerCasedString.charAt(i))
            {
                ci.setIndex(savedIdx);
                return false;
            }

            c = ci.next();
        }

        return true;
    }

    /**
     * Check if String "s" is found ignoring the case, and not moving the cursor position.
     * @param s the String to find
     * @return true if string is found
     */
    public boolean peekStringIgnoreCase(String s)
    {
        String lowerCasedString = s.toLowerCase();
        
        int savedIdx = skipWS();

        int len = lowerCasedString.length();
        char c = ci.current(); 

        for (int i = 0; i < len; ++i)
        {
            if (Character.toLowerCase(c) != lowerCasedString.charAt(i))
            {
                ci.setIndex(savedIdx);
                return false;
            }
            c = ci.next();
        }
        ci.setIndex(savedIdx);

        return true;
    }

    /**
     * Parse a java identifier from the current position.
     * @return The identifier
     */
    public String parseIdentifier()
    {
        skipWS();
        char c = ci.current();

        if (!Character.isJavaIdentifierStart(c) && parameterPrefixes.indexOf(c) < 0)
        {
            // Current character is not a valid identifier char, and isn't a valid parameter prefix
            return null;
        }

        StringBuilder id = new StringBuilder();
        id.append(c);
        while (Character.isJavaIdentifierPart(c = ci.next()))
        {
            id.append(c);
        }

        return id.toString();
    }

    /**
     * Checks if a java Method is found
     * @return true if a Method is found
     */
    public String parseMethod()
    {
        int savedIdx = ci.getIndex();
        
        String id;

        if ((id = parseIdentifier()) == null)
        {
            ci.setIndex(savedIdx);
            return null;
        }
        
        skipWS();
        
        if (!parseChar('(') )
    	{
            ci.setIndex(savedIdx);
            return null;
    	}
        ci.setIndex(ci.getIndex()-1);        
        return id;
    }

    /**
     * Parses the text string (up to the next space) and
     * returns it. The name includes '.' characters.
     * This can be used, for example, when parsing a class name wanting to
     * read in the full name (including package) so that it can then be
     * checked for existence in the CLASSPATH.
     * @return The name
     */
    public String parseName()
    {
        int savedIdx = skipWS();
        String id;

        if ((id = parseIdentifier()) == null)
        {
            return null;
        }

        StringBuilder qn = new StringBuilder(id);

        while (parseChar('.'))
        {
            if ((id = parseIdentifier()) == null)
            {
                ci.setIndex(savedIdx);
                return null;
            }

            qn.append('.').append(id);
        }

        return qn.toString();
    }

    /**
     * Parse a cast in the query from the current position, returning
     * the name of the class that is being cast to. 
     * Returns null if the current position doesn't have a cast.
     * Does no checking as to whether the name is a valid class name, just whether there is
     * "({name})" from the current position.
     * @return The name of the class to cast to
     */
    public String parseCast()
    {
        int savedIdx = skipWS();
        String typeName;

        if (!parseChar('(') || (typeName = parseName()) == null || !parseChar(')'))
        {
            ci.setIndex(savedIdx);
            return null;
        }

        return typeName;
    }

    /**
     * Utility to return if a character is a decimal digit.
     * @param c The character
     * @return Whether it is a decimal digit
     */
    private final static boolean isDecDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    /**
     * Utility to return if a character is a octal digit.
     * @param c The character
     * @return Whether it is a octal digit
     */
    private final static boolean isOctDigit(char c)
    {
        return c >= '0' && c <= '7';
    }

    /**
     * Utility to return if a character is a hexadecimal digit.
     * @param c The character
     * @return Whether it is a hexadecimal digit
     */
    private final static boolean isHexDigit(char c)
    {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    /**
     * Parse an integer number from the current position.
     * @return The integer number parsed (null if not valid).
     */
    public BigInteger parseIntegerLiteral()
    {
        int savedIdx = skipWS();

        StringBuilder digits = new StringBuilder();
        int radix;
        char c = ci.current();
        boolean negate = false;
        if (c == '-')
        {
            negate = true;
            c = ci.next();
        }

        if (c == '0')
        {
            c = ci.next();

            if (c == 'x' || c == 'X')
            {
                radix = 16;
                c = ci.next();

                while (isHexDigit(c))
                {
                    digits.append(c);
                    c = ci.next();
                }
            }
            else if (isOctDigit(c))
            {
                radix = 8;

                do
                {
                    digits.append(c);
                    c = ci.next();
                } while (isOctDigit(c));
            }
            else
            {
                radix = 10;
                digits.append('0');
            }
        }
        else
        {
            radix = 10;

            while (isDecDigit(c))
            {
                digits.append(c);
                c = ci.next();
            }
        }

        if (digits.length() == 0)
        {
            ci.setIndex(savedIdx);
            return null;
        }

        if (c == 'l' || c == 'L')
        {
            ci.next();
        }

        if (negate)
        {
            return new BigInteger(digits.toString(), radix).negate();
        }
        else
        {
            return new BigInteger(digits.toString(), radix);
        }
    }

    /**
     * Parse a floating point number from the current position.
     * @return The floating point number parsed (null if not valid).
     */
    public BigDecimal parseFloatingPointLiteral()
    {
        int savedIdx = skipWS();
        StringBuilder val = new StringBuilder();
        boolean dotSeen = false;
        boolean expSeen = false;
        boolean sfxSeen = false;

        char c = ci.current();
        boolean negate = false;
        if (c == '-')
        {
            negate = true;
            c = ci.next();
        }

        while (isDecDigit(c))
        {
            val.append(c);
            c = ci.next();
        }

        if (c == '.')
        {
            dotSeen = true;
            val.append(c);
            c = ci.next();

            while (isDecDigit(c))
            {
                val.append(c);
                c = ci.next();
            }
        }

        if (val.length() < (dotSeen ? 2 : 1))
        {
            ci.setIndex(savedIdx);
            return null;
        }

        if (c == 'e' || c == 'E')
        {
            expSeen = true;
            val.append(c);
            c = ci.next();

            if (c != '+' && c != '-' && !isDecDigit(c))
            {
                ci.setIndex(savedIdx);
                return null;
            }

            do
            {
                val.append(c);
                c = ci.next();
            } while (isDecDigit(c));
        }

        if (c == 'f' || c == 'F' || c == 'd' || c == 'D')
        {
            sfxSeen = true;
            ci.next();
        }

        if (!dotSeen && !expSeen && !sfxSeen)
        {
            ci.setIndex(savedIdx);
            return null;
        }

        if (negate)
        {
            return new BigDecimal(val.toString()).negate();
        }
        else
        {
            return new BigDecimal(val.toString());
        }
    }

    /**
     * Parse a boolean from the current position.
     * @return The boolean parsed (null if not valid).
     */
    public Boolean parseBooleanLiteral()
    {
        int savedIdx = skipWS();
        String id;

        if ((id = parseIdentifier()) == null)
        {
            return null;
        }

        if (id.equals("true"))
        {
             return Boolean.TRUE;
        }
        else if (id.equals("false"))
        {
            return Boolean.FALSE;
        }
        else
        {
            ci.setIndex(savedIdx);
            return null;
        }
    }

    /**
     * Parse a boolean from the current position (case insensitive).
     * @return The boolean parsed (null if not valid).
     */
    public Boolean parseBooleanLiteralIgnoreCase()
    {
        int savedIdx = skipWS();
        String id;

        if ((id = parseIdentifier()) == null)
        {
            return null;
        }

        if (id.equalsIgnoreCase("true"))
        {
             return Boolean.TRUE;
        }
        else if (id.equalsIgnoreCase("false"))
        {
            return Boolean.FALSE;
        }
        else
        {
            ci.setIndex(savedIdx);
            return null;
        }
    }

    /**
     * Utility to return if the next non-whitespace character is a single quote.
     * @return Whether it is a single quote at the current point (ignoring whitespace)
     */
    public boolean nextIsSingleQuote()
    {
        skipWS();
        return (ci.current() == '\'');
    }

    /**
     * Utility to return if the next character is a dot.
     * @return Whether it is a dot at the current point
     */
    public boolean nextIsDot()
    {
        return (ci.current() == '.');
    }

    /**
     * Parse a Character literal
     * @return the Character parsed. null if single quotes is found
     * @throws NucleusUserException if an invalid character is found or the CharacterIterator is finished
     */
    public Character parseCharacterLiteral()
    {
        skipWS();

        if (ci.current() != '\'')
        {
            return null;
        }

        char c = ci.next();

        if (c == CharacterIterator.DONE)
        {
            throw new NucleusUserException("Invalid character literal: " + input);
        }

        // Not needed for JPQL, and almost certainly not for JDOQL
        if (parseEscapedChars && c == '\\')
        {
            c = parseEscapedCharacter();
        }

        if (ci.next() != '\'')
        {
            throw new NucleusUserException("Invalid character literal: " + input);
        }

        ci.next();

        return Character.valueOf(c);
    }

    /**
     * Parse a String literal
     * @return the String parsed. null if single quotes or double quotes is found
     * @throws NucleusUserException if an invalid character is found or the CharacterIterator is finished
     */
    public String parseStringLiteral()
    {
        skipWS();

        // Strings can be surrounded by single or double quotes
        char quote = ci.current();
        if (quote != '"' && quote != '\'')
        {
            return null;
        }

        StringBuilder lit = new StringBuilder();
        char c;

        while ((c = ci.next()) != quote)
        {
            if (c == CharacterIterator.DONE)
            {
                throw new NucleusUserException("Invalid string literal (End of stream): " + input);
            }

            // Not needed for JPQL, and almost certainly not for JDOQL
            if (parseEscapedChars && c == '\\')
            {
                c = parseEscapedCharacter();
            }

            lit.append(c);
        }

        ci.next();

        return lit.toString();
    }

    /**
     * Parse an escaped character.
     * @return the escaped char
     * @throws NucleusUserException if a escaped character is not valid
     */
    private char parseEscapedCharacter()
    {
        char c;

        if (isOctDigit(c = ci.next()))
        {
            int i = (c - '0');

            if (isOctDigit(c = ci.next()))
            {
                i = i * 8 + (c - '0');

                if (isOctDigit(c = ci.next()))
                {
                    i = i * 8 + (c - '0');
                }
                else
                {
                    ci.previous();
                }
            }
            else
            {
                ci.previous();
            }

            if (i > 0xff)
            {
                throw new NucleusUserException("Invalid character escape: '\\" + Integer.toOctalString(i) + "'");
            }

            return (char)i;
        }
        else
        {
            switch (c)
            {
                case 'b':   return '\b';
                case 't':   return '\t';
                case 'n':   return '\n';
                case 'f':   return '\f';
                case 'r':   return '\r';
                case '"':   return '"';
                case '\'':  return '\'';
                case '\\':  return '\\';
                default:
                    throw new NucleusUserException("Invalid character escape: '\\" + c + "'");
            }
        }
    }

    /**
     * Checks if null literal is parsed
     * @return true if null literal is found
     */
    public boolean parseNullLiteral()
    {
        int savedIdx = skipWS();
        String id;

        if ((id = parseIdentifier()) == null)
        {
            return false;
        }
        else if (id.equals("null"))
        {
            return true;
        }
        else
        {
            ci.setIndex(savedIdx);
            return false;
        }
    }

    /**
     * Checks if null literal is parsed (case insensitive).
     * @return true if null literal is found
     */
    public boolean parseNullLiteralIgnoreCase()
    {
        int savedIdx = skipWS();
        String id;

        if ((id = parseIdentifier()) == null)
        {
            return false;
        }
        else if (id.equalsIgnoreCase("null"))
        {
            return true;
        }
        else
        {
            ci.setIndex(savedIdx);
            return false;
        }
    }

    /**
     * Method to return the remaining part of the string not yet processed.
     * Doesn't move the current position.
     * @return The remaining part of the string
     */
    public String remaining()
    {
        int position = ci.getIndex();
        StringBuilder sb = new StringBuilder();
        char c = ci.current();
        while (c != CharacterIterator.DONE)
        {
            sb.append(c);
            c = ci.next();
        }
        ci.setIndex(position);
        return sb.toString();
    }
    
    public String toString()
    {
        return input;
    }
}