/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved. 
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
2003 Andy Jefferson - commented
    ...
**********************************************************************/
package org.datanucleus.util;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;

/**
 * Macro String Utilities
 */
public class MacroString
{
    protected static final Localiser LOCALISER=Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    private final String  thisClassName;
    private final Imports imports;
    private final String  macroString;

    /**
     * Constructor.
     * @param className The class name
     * @param importsString String containing IMPORTs
     * @param macroString   String containing macro
     **/
    public MacroString(String className, String importsString, String macroString)
    {
        this.thisClassName = className;
        this.macroString = macroString;

        imports = new Imports();
        if (thisClassName != null)
        {
            imports.importPackage(thisClassName);
        }
        if (importsString != null)
        {
            imports.parseImports(importsString);
        }
    }

    /**
     * Utility to substitute macros using the supplier handler.
     * @param mh Macro handler.
     * @param clr ClassLoaderResolver
     * @return The updated string
     **/
    public String substituteMacros(MacroHandler mh, ClassLoaderResolver clr)
    {
        StringBuilder outBuf = new StringBuilder();
        int left, right;

        /* Pass 1: Substitute SQL identifier macros "{...}". */
        for (int curr = 0; curr < macroString.length(); curr = right + 1)
        {
            if ((left = macroString.indexOf('{', curr)) < 0)
            {
                outBuf.append(macroString.substring(curr));
                break;
            }

            outBuf.append(macroString.substring(curr, left));

            if ((right = macroString.indexOf('}', left + 1)) < 0)
            {
                throw new NucleusUserException(LOCALISER.msg("031000",macroString));
            }

            IdentifierMacro im = parseIdentifierMacro(macroString.substring(left + 1, right), clr);
            mh.onIdentifierMacro(im);

            outBuf.append(im.value);
        }

        String tmpString = outBuf.toString();
        outBuf = new StringBuilder();

        /* Pass 2: Substitute parameter macros "?...?". */
        for (int curr = 0; curr < tmpString.length(); curr = right + 1)
        {
            if ((left = tmpString.indexOf('?', curr)) < 0)
            {
                outBuf.append(tmpString.substring(curr));
                break;
            }

            outBuf.append(tmpString.substring(curr, left));

            if ((right = tmpString.indexOf('?', left + 1)) < 0)
            {
                throw new NucleusUserException(LOCALISER.msg("031001",tmpString));
            }

            ParameterMacro pm = new ParameterMacro(tmpString.substring(left + 1, right));
            mh.onParameterMacro(pm);

            outBuf.append('?');
        }

        return outBuf.toString();
    }

    /**
     * Utility to resolve a class declaration.
     * @param className Name of the class
     * @param clr the ClassLoaderResolver
     * @return The class
     **/
    private Class resolveClassDeclaration(String className, ClassLoaderResolver clr)
    {
        try
        {
            return className.equals("this") ? clr.classForName(thisClassName,null) : imports.resolveClassDeclaration(className, clr, null);
        }
        catch (ClassNotResolvedException e)
        {
            return null;
        }
    }

    /**
     * Utility to parse the identitifer macro.
     * @param unparsed The unparsed string
     * @param clr ClassLoaderResolver
     * @return The parsed string
     **/
    private IdentifierMacro parseIdentifierMacro(String unparsed, ClassLoaderResolver clr)
    {
        String className = null;
        String fieldName = null;
        String subfieldName = null;

        Class c = resolveClassDeclaration(unparsed, clr);

        if (c == null)
        {
            /* The last '.' in the string. */
            int lastDot = unparsed.lastIndexOf('.');

            if (lastDot < 0)
            {
                throw new NucleusUserException(LOCALISER.msg("031002",unparsed));
            }

            fieldName = unparsed.substring(lastDot + 1);
            className = unparsed.substring(0, lastDot);
            c = resolveClassDeclaration(className, clr);

            if (c == null)
            {
                /* The second to the last dot in the string. */
                int lastDot2 = unparsed.lastIndexOf('.', lastDot - 1);

                if (lastDot2 < 0)
                {
                    throw new NucleusUserException(LOCALISER.msg("031002",unparsed));
                }

                subfieldName = fieldName;
                fieldName    = unparsed.substring(lastDot2 + 1, lastDot);
                className    = unparsed.substring(0, lastDot2);
                c = resolveClassDeclaration(className, clr);

                if (c == null)
                {
                    throw new NucleusUserException(LOCALISER.msg("031002",unparsed));
                }
            }
        }

        return new IdentifierMacro(unparsed, c.getName(), fieldName, subfieldName);
    }

    /**
     * Inner class : Identifier Macro
     **/
    public static class IdentifierMacro
    {
        /** unparsed identifier macro **/
        public final String unparsed;
        /** the class name **/
        public final String className;
        /** the field name **/
        public final String fieldName;
        /** the sub field name TODO what is this **/
        public final String subfieldName;
        /** the value **/
        public String value;

        /**
         * Constructor
         * @param unparsed the unparsed macro
         * @param className the class name
         * @param fieldName the field name
         * @param subfieldName the sub field name TODO what is this
         */
        IdentifierMacro(String unparsed, String className, String fieldName, String subfieldName)
        {
            this.unparsed     = unparsed;
            this.className    = className;
            this.fieldName    = fieldName;
            this.subfieldName = subfieldName;
            this.value        = null;
        }

        public String toString()
        {
            return "{" + unparsed + "}";
        }
    }

    /**
     * Inner class : Parameter Macro
     **/
    public static class ParameterMacro
    {
        /** the parameter name **/
        public final String parameterName;

        /**
         * Constructor
         * @param parameterName the parameter name
         */
        ParameterMacro(String parameterName)
        {
            this.parameterName = parameterName;
        }

        public String toString()
        {
            return "?" + parameterName + "?";
        }
    }

    /**
     * Inner class : Macro Handler
     **/
    public static interface MacroHandler
    {
        /**
         * handles identifier macros
         * @param im the identitifier macro to handle
         */
        void onIdentifierMacro(IdentifierMacro im);
        
        /**
         * handler parameter macros
         * @param pm the parameter macro to handle
         */
        void onParameterMacro(ParameterMacro pm);
    }
}
