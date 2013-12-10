/**********************************************************************
Copyright (c) 2003 Andy Jefferson and others. All rights reserved. 
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
2004 Erik Bengtson - added exception methods
    ...
**********************************************************************/
package org.datanucleus.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Resource Bundle manager, providing simplified means of using MessageFormat to localise the system.
 */
public class Localiser
{
    /** Convenience flag whether to display numbers in messages. */
    private static boolean displayCodesInMessages = false;

    private static Hashtable<String, Localiser> helpers = new Hashtable();

    private static Locale locale = Locale.getDefault();

    private static Hashtable<String, MessageFormat> msgFormats = new Hashtable();

    /** The underlying bundle. */
    private ResourceBundle bundle = null;

    private ClassLoader loader = null;

    /**
     * Private constructor to prevent outside instantiation. Operates on a
     * principle of one helper per bundle.
     * @param bundleName the name of the resource bundle
     * @param classLoader the class loader from which to load the resource
     * bundle
     */
    private Localiser(String bundleName, ClassLoader classLoader)
    {
        // We can assume that the bundle hasn't been loaded since no helper has been found for it
        try
        {
            loader = classLoader;
            bundle = ResourceBundle.getBundle(bundleName, locale, classLoader);
        }
        catch (MissingResourceException mre)
        {
            NucleusLogger.GENERAL.error("ResourceBundle " + bundleName + " for locale " + locale + " was not found!");
        }
    }

    /**
     * Convenience method to change the language of the resource bundles used by the localisers.
     * @param languageCode The new language code
     */
    public static synchronized void setLanguage(String languageCode)
    {
        if (languageCode == null)
        {
            return;
        }
        if (locale.getLanguage().equalsIgnoreCase(languageCode))
        {
            // Already in this language
            return;
        }

        NucleusLogger.GENERAL.info("Setting localisation to " + languageCode + " from " + locale.getLanguage());
        locale = new Locale(languageCode);

        // Change language of all Localiser objects to this language
        Set<String> bundleNames = helpers.keySet();
        Iterator<String> bundleIter = bundleNames.iterator();
        while (bundleIter.hasNext())
        {
            String bundleName = bundleIter.next();
            Localiser localiser = helpers.get(bundleName);
            try
            {
                ClassLoader loader = localiser.loader;
                localiser.bundle = ResourceBundle.getBundle(bundleName, locale, loader);
            }
            catch (MissingResourceException mre)
            {
                NucleusLogger.GENERAL.error("ResourceBundle " + bundleName + " for locale " + locale + " was not found!");
            }
        }
    }

    /**
     * Method to allow turning on/off of display of error codes in messages.
     * @param display Whether to display codes
     */
    public static void setDisplayCodesInMessages(boolean display)
    {
        NucleusLogger.GENERAL.info("Setting localisation codes display to " + display);
        displayCodesInMessages = display;
    }

    /**
     * Accessor for a helper instance for a bundle.
     * @param bundle_name the name of the bundle
     * @param class_loader the class loader from which to load the resource
     * bundle
     * @return the helper instance bound to the bundle
     */
    public static Localiser getInstance(String bundle_name, ClassLoader class_loader)
    {
        Localiser localiser = helpers.get(bundle_name);
        if (localiser != null)
        {
            return localiser;
        }
        localiser = new Localiser(bundle_name,class_loader);
        helpers.put(bundle_name,localiser);

        return localiser;
    }

    /**
     * Message formatter for an internationalised message.
     * @param includeCode Whether to include the code in the message
     * @param messageKey the message key
     * @return the resolved message text
     */
    public String msg(boolean includeCode, String messageKey)
    {
        return getMessage(includeCode, bundle, messageKey, null);
    }

    /**
     * Message formatter with one argument passed in that will be embedded in an internationalised message.
     * @param includeCode Whether to include the code in the message
     * @param messageKey the message key
     * @param arg the argument
     * @return the resolved message text
     */
    public String msg(boolean includeCode, String messageKey, long arg)
    {
        Object[] args = {String.valueOf(arg)};
        return getMessage(true, bundle, messageKey, args);
    }

    /**
     * Message formatter with one argument passed in that will be embedded in an internationalised message.
     * @param includeCode Whether to include the code in the message
     * @param messageKey the message key
     * @param arg1 the first argument
     * @return the resolved message text
     */
    public String msg(boolean includeCode, String messageKey, Object arg1)
    {
        Object[] args={arg1};
        return getMessage(includeCode, bundle, messageKey, args);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param includeCode Whether to include the code in the message
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return the resolved message text
     */
    public String msg(boolean includeCode, String messageKey, Object arg1, Object arg2)
    {
        Object[] args={arg1,arg2};
        return getMessage(includeCode, bundle, messageKey, args);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param includeCode Whether to include the code in the message
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @return the resolved message text
     */
    public String msg(boolean includeCode, String messageKey, Object arg1, Object arg2, Object arg3)
    {
        Object[] args={arg1,arg2,arg3};
        return getMessage(includeCode, bundle, messageKey, args);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param includeCode Whether to include the code in the message
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the third argument
     * @return the resolved message text
     */
    public String msg(boolean includeCode, String messageKey, Object arg1, Object arg2, Object arg3, Object arg4)
    {
        Object[] args={arg1,arg2,arg3,arg4};
        return getMessage(includeCode, bundle, messageKey, args);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param includeCode Whether to include the code in the message
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the third argument
     * @param arg5 the third argument
     * @return the resolved message text
     */
    public String msg(boolean includeCode, String messageKey, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
    {
        Object[] args={arg1,arg2,arg3,arg4,arg5};
        return getMessage(includeCode, bundle, messageKey, args);
    }    

    /**
     * Message formatter for an internationalised message.
     * @param messageKey the message key
     * @return the resolved message text
     */
    public String msg(String messageKey)
    {
    	return msg(true, messageKey);
    }

    /**
     * Message formatter with one argument passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param arg1 the first argument
     * @return the resolved message text
     */
    public String msg(String messageKey, Object arg1)
    {
        return msg(true, messageKey, arg1);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return the resolved message text
     */
    public String msg(String messageKey, Object arg1, Object arg2)
    {
        return msg(true, messageKey, arg1, arg2);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @return the resolved message text
     */
    public String msg(String messageKey, Object arg1, Object arg2, Object arg3)
    {
        return msg(true, messageKey, arg1, arg2, arg3);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the third argument
     * @return the resolved message text
     */
    public String msg(String messageKey, Object arg1, Object arg2, Object arg3, Object arg4)
    {
        return msg(true, messageKey, arg1, arg2, arg3, arg4);
    }

    /**
     * Message formatter with a series of arguments passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the third argument
     * @param arg5 the third argument
     * @return the resolved message text
     */
    public String msg(String messageKey, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
    {
        return msg(true, messageKey, arg1, arg2, arg3, arg4, arg5);
    }

    /**
     * Message formatter with one argument passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param arg the argument
     * @return the resolved message text
     */
    public String msg(String messageKey, long arg)
    {
        return msg(true, messageKey, arg);
    }
    
    /**
     * Method to provide the MessageFormat logic. All of the msg()
     * methods are wrappers to this one.
     * @param bundle the resource bundle
     * @param messageKey the message key
     * @param msgArgs an array of arguments to substitute into the message
     * @return the resolved message text
     */
    final private static String getMessage(boolean includeCode, ResourceBundle bundle, String messageKey, Object msgArgs[]) 
    {
        if (messageKey == null)
        {
            NucleusLogger.GENERAL.error("Attempt to retrieve resource with NULL name !");
            return null;
        }

        if (msgArgs != null)
        {
            for (int i=0; i<msgArgs.length; i++)
            {
                if (msgArgs[i] == null)
                {
                    msgArgs[i] = "";
                }
                if (Throwable.class.isAssignableFrom(msgArgs[i].getClass()))
                { 
                    msgArgs[i] = getStringFromException((Throwable)msgArgs[i]);
                }
            }
        }
        try
        {
            String stringForKey = bundle.getString(messageKey);
            if (includeCode && displayCodesInMessages)
            {
                // Provide coded message "[DN-012345] ..."
                char c = messageKey.charAt(0);
                if (c >= '0' && c<= '9')
                {
                    stringForKey = "[DN-" + messageKey + "] " + stringForKey;
                }
            }

            if (msgArgs != null)
            {
                MessageFormat formatter = msgFormats.get(stringForKey);
                if (formatter == null)
                {
                    formatter = new MessageFormat(stringForKey);
                    msgFormats.put(stringForKey,formatter);
                }
                return formatter.format(msgArgs);
            }
            else
            {
                return stringForKey;
            }
        }
        catch (MissingResourceException mre)
        {
            NucleusLogger.GENERAL.error("Parameter " + messageKey + " doesn't exist for bundle " + bundle);
        }

        return null;
    }

    /**
     * Gets a String message from Exceptions. This method transforms nested exceptions into printable messages.
     * @param exception to be read and transformed into a messsage to print
     * @return the message to output
     */
    private static String getStringFromException(java.lang.Throwable exception)
    {
        StringBuilder msg = new StringBuilder();
        if (exception != null)
        {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            printWriter.close();
            try
            {
                stringWriter.close();
            }
            catch(Exception e)
            {
                //do nothing
            }
            msg.append(exception.getMessage());
            msg.append('\n');
            msg.append(stringWriter.toString());
            
            // JDBC: SQLException
            if (exception instanceof SQLException)
            {
                if (((SQLException) exception).getNextException() != null)
                {
                    msg.append('\n');
                    msg.append(getStringFromException(((SQLException) exception).getNextException()));
                }
            }
            // Reflection: InvocationTargetException
            else if (exception instanceof InvocationTargetException)
            {
                if (((InvocationTargetException) exception).getTargetException() != null)
                {
                    msg.append('\n');
                    msg.append(getStringFromException(((InvocationTargetException) exception).getTargetException()));
                }
            }
            
            // Add more exceptions here, so we can provide a relly complete information at the log
        }
        return msg.toString();
    }
}