/**********************************************************************
Copyright (c) 2004 Kikuchi Kousuke and others. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Command line option parser.
 * Used by command line applications such as Enhancer and SchemaTool to process command line arguments.
 */
public class CommandLine
{
    /** Appended options */
    protected HashMap<String, Option> options = new HashMap();

    /** Appended options */
    protected HashMap valueOptions = new HashMap();

    /** Appended options */
    protected ArrayList optionList = new ArrayList();

    /** Default arguments */
    protected String defaultArg[];

    /** displays dash **/
    protected boolean displaysDash = true;
    
    /**
     * CommandLine option base class
     */
    protected static class Option
    {
        /** Short name option */
        final String shortName;
        /** Long name option */
        final String longName;
        /** option description */
        final String description;

        /**
         * Constructor 
         * @param shortName  Short name option(exclude "-")
         * @param longName Long name option(exclude "--")
         * @param desc option description
         */
        public Option(String shortName, String longName, String desc)
        {
            this.shortName = shortName;
            this.longName = longName;
            this.description = desc;
        }
    }

    /**
     * No argument option class.
     **/
    protected static class NoArgOption extends Option
    {
        /** designated */
        boolean selected;
        /**
         * Constructor 
         * @param shortName  Short name option(exclude "-")
         * @param longName Long name option(exclude "--")
         * @param desc option description
         */
        public NoArgOption(String shortName, String longName, String desc)
        {
            super(shortName, longName, desc);
        }
    }

    /**
     * Use argment option class.
     **/
    protected static class WithArgOption extends Option
    {
        /** Option argment name */
        String name;
        /** Designated argment value */
        String option;
        /**
         * Constructor 
         * @param shortName  Short name option(exclude "-")
         * @param longName Long name option(exclude "--")
         * @param desc option description
         * @param name argment name
         */
        public WithArgOption(String shortName, String longName, String desc, String name)
        {
            super(shortName, longName, desc);
            this.name = name;
        }
    }

    /**
     * Default constructor
     **/
    public CommandLine()
    {
        //default constructor
    }

    /**
     * Default constructor
     * @param displaysDash whether to display a dash in the short name
     **/
    public CommandLine(boolean displaysDash)
    {
        this.displaysDash = displaysDash;
    }

    /**
     * Add new Option.
     * <br>
     * If argName is null, set this option no-arg option.
     * @param shortName Short name option eg "d"
     * @param longName Long name option eg "directory"
     * @param argName Argment name. No argment option if this param is null.
     * @param desc Desription this option. 
     */
    public void addOption(String shortName, String longName, String argName, String desc)
    {
        Option option = null;
        if (StringUtils.isEmpty(shortName) && StringUtils.isEmpty(longName))
        {
            throw new IllegalArgumentException("require shortName or longName");
        }

        if (StringUtils.notEmpty(argName))
        {
            option = new WithArgOption(shortName, longName, desc, argName);
        }
        else
        {
            option = new NoArgOption(shortName, longName, desc);
        }

        optionList.add(option);
        if (StringUtils.notEmpty(shortName))
        {
            options.put("-" + shortName, option);
            valueOptions.put(shortName, option);
        }

        if (StringUtils.notEmpty(longName))
        {
            options.put("--" + longName, option);
            valueOptions.put(longName, option);
        }
    }

    /**
     * Parse command line argments.
     * @param args Command line argments
     */
    public void parse(String args[])
    {
        ArrayList defaultArg = new ArrayList();
        if ((args == null || (args.length == 0)))
        {
            return;
        }
        int i = 0;
        while (i < args.length)
        {
            if (StringUtils.isEmpty(args[i]))
            {
                //do nothing
            }
            else if (args[i].startsWith("-"))
            {
                if (options.containsKey(args[i]))
                {
                    Option option = options.get(args[i]);
                    if (option instanceof NoArgOption)
                    {
                        ((NoArgOption)option).selected = true;
                    }
                    else
                    {
                        if (args.length - 1 == i)
                        {
                            throw new RuntimeException("option " + args[i] + " needs an argument");
                        }
                        else
                        {
                            ((WithArgOption)option).option = args[i + 1];
                            i++;
                        }
                    }
                }
                else
                {
                    defaultArg.add(args[i]);
                }
            }
            else
            {
                defaultArg.add(args[i]);
            }

            i++;
        }

        if (defaultArg.size() == 0)
        {
            this.defaultArg = new String[0];
        }
        String result[] = new String[defaultArg.size()];
        for (i = 0; i < result.length; i++)
        {
            result[i] = (String)defaultArg.get(i);
        }
        this.defaultArg = result;
    }

    /**
     * Check option selected
     * @param name Option name (both short name and long name ok)
     * @return Return true, if option selected.
     */
    public boolean hasOption(String name)
    {
        if (!valueOptions.containsKey(name))
        {
            throw new IllegalArgumentException("no such option " + name);
        }
        Option option = (Option)valueOptions.get(name);
        if (option instanceof NoArgOption)
        {
            return ((NoArgOption)option).selected;
        }
        return StringUtils.notEmpty(((WithArgOption)option).option);
    }

    /**
     * Return option argument.
     * @param name Option name (both short name and long name ok)
     * @return option Argument
     * @throws IllegalArgumentException If unmanaged name recieved.
     */
    public String getOptionArg(String name)
    {
        if (!valueOptions.containsKey(name))
        {
            throw new IllegalArgumentException("no such option " + name);
        }
        Option option = (Option)valueOptions.get(name);
        if (option instanceof NoArgOption)
        {
            return "" + ((NoArgOption)option).selected;
        }
        return ((WithArgOption)option).option;
    }

    /**
     * Return string like useage.
     * @return Useage string
     */
    public String toString()
    {
        if (optionList.size() == 0)
        {
            return "[NO OPTIONS]";
        }
        int maxLength = 80;
        StringBuilder sb = new StringBuilder();
        int shortMax = 0;
        int longMax = 0;
        int argNameMax = 0;
        int descMax = 0;
        for (int i = 0; i < optionList.size(); i++)
        {
            Option o = (Option)optionList.get(i);
            if (o.shortName != null)
            {
                if ((o.shortName != null) && (o.shortName.length() > shortMax))
                {
                    shortMax = o.shortName.length();
                }

                if ((o.longName != null) && (o.longName.length() > longMax))
                {
                    longMax = o.longName.length();
                }

                if (o instanceof WithArgOption)
                {
                    WithArgOption op = (WithArgOption)o;
                    if (op.name.length() > argNameMax)
                    {
                        argNameMax = op.name.length();
                    }
                }

                if ((o.description != null) && (o.description.length() > descMax))
                {
                    descMax = o.description.length();
                }
            }
        }
        if (shortMax > 0)
        {
            shortMax += 3;
        }
        if (longMax > 0)
        {
            longMax += 3;
        }
        if (argNameMax > 0)
        {
            argNameMax += 3;
        }
        for (int i = 0; i < optionList.size(); i++)
        {
            int j = 0;
            Option o = (Option)optionList.get(i);
            if (StringUtils.notEmpty(o.shortName))
            {
                if( this.displaysDash )
                {
                    sb.append("-");
                }
                sb.append(o.shortName);
                j = o.shortName.length() + 1;
            }
            for (; j < shortMax; j++)
            {
                sb.append(" ");
            }

            j = 0;
            if (StringUtils.notEmpty(o.longName))
            {
                sb.append("--");
                sb.append(o.longName);
                j = o.longName.length() + 2;
            }
            for (; j < longMax; j++)
            {
                sb.append(" ");
            }

            j = 0;
            if (o instanceof WithArgOption)
            {
                WithArgOption op = (WithArgOption)o;
                sb.append(op.name);
                j = op.name.length();
            }
            for (; j < argNameMax; j++)
            {
                sb.append(" ");
            }

            if (StringUtils.notEmpty(o.description))
            {
                int basePos;
                if ((shortMax + longMax + argNameMax) > maxLength)
                {
                    basePos = maxLength / 2;
                    sb.append("\n");
                    for (int k = 0; k < basePos; k++)
                    {
                        sb.append(" ");
                    }
                }
                else
                {
                    basePos = (shortMax + longMax + argNameMax);
                }
                int pos = basePos;
                for (j = 0; j < o.description.length(); j++)
                {
                    sb.append(o.description.charAt(j));
                    if (pos >= maxLength)
                    {
                        if( j<o.description.length()-1 && o.description.charAt(j+1)!=' ')
                        {
                            //just do not break work in the middle, and wrap word to next line
                            for( int p=sb.length()-1; p>=0; p--)
                            {
                                if( sb.charAt(p)==' ')
                                {
                                    sb.insert(p, '\n');
                                    for (int k = 0; k < basePos-1; k++)
                                    {
                                        sb.insert(p+1," ");
                                    }
                                    break;
                                }
                            }
                        }
                        else
                        {
                            sb.append("\n");
                            for (int k = 0; k < basePos; k++)
                            {
                                sb.append(" ");
                            }
                        }
                        pos = basePos;
                    }
                    pos++;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Return default arguments.
     * @return Default arguments
     */
    public String[] getDefaultArgs()
    {
        return this.defaultArg;
    }
}