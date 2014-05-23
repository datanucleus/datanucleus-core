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
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ClassConstants;

/**
 * Localiser for messages in the DataNucleus system.
 * Provides access to a singleton, via the <pre>getInstance</pre> method.
 * Any plugin that provides its own resources via a properties file should call
 * <pre>Localiser.registerBundle(...)</pre> before the resources are required to be used.
 * <p>
 * The DataNucleus system is internationalisable hence messages (to log files or exceptions) can
 * be displayed in multiple languages. Currently DataNucleus contains localisation files in the 
 * English and Spanish, but can be extended easily by adding localisation files in languages such as French, etc. 
 * The internationalisation operates around a Java ResourceBundle, loading a properties file.
 * 
 * Messages are output via calling
 * <pre>Localiser.msg("012345", args);</pre>
 * The messages themselves are contained in a file for each package. For example, with the above example,
 * we have "org.datanucleus.Localisation.properties". This contains entries such as
 * <pre>012345=Initialising Schema "{0}" using "{1}" auto-start option</pre>
 * So the 2 parameters specified in the <pre>Localiser.msg(...)</pre> call are inserted into the message. 
 * <h3>Extending for other languages</h3>
 * The language-specific parts are always contained in the Localisation.properties file.
 * To extend the current system to internationalise in, for example, French you would add a file 
 * "org.datanucleus.Localisation_fr.properties" and add entries with the required French text.
 * If you want to extend this to another language and contribute the files for your language
 * you need to find all files "Localisation.properties" and provide an alternative variant, raise an issue in JIRA, and
 * attach your patch to the issue.
 */
public class Localiser
{
    private static Locale locale = null;

    /** Convenience flag whether to display numbers in messages. */
    private static boolean displayCodesInMessages = false;

    private static Map<String, String> properties = new ConcurrentHashMap<String, String>();

    private static Hashtable<String, MessageFormat> msgFormats = new Hashtable();

    static
    {
        // User can specify the language of messages for this JVM via a System property
        String language = System.getProperty("datanucleus.localisation.language");
        if (language == null)
        {
            locale = Locale.getDefault();
        }
        else
        {
            locale = new Locale(language);
        }

        // User can set whether to use messageCodes in messages for this JVM via a System property
        String messageCodes = System.getProperty("datanucleus.localisation.messageCodes");
        if (messageCodes != null)
        {
            displayCodesInMessages = Boolean.parseBoolean(messageCodes);
        }

        // Register the primary resource bundle
        registerBundle("org.datanucleus.Localisation", ClassConstants.NUCLEUS_CONTEXT_LOADER);
    }

    /**
     * Method to be called by plugins that have their own ResourceBundle, so the messages will be registered
     * and available for use.
     * @param bundleName Name of the bundle e.g "org.datanucleus.store.myplugin.Localisation"
     * @param loader Loader for the bundle
     */
    public static void registerBundle(String bundleName, ClassLoader loader)
    {
        try
        {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale, loader);
            for (String key : bundle.keySet())
            {
                properties.put(key, bundle.getString(key));
            }
        }
        catch (MissingResourceException mre)
        {
            NucleusLogger.GENERAL.error("ResourceBundle " + bundleName + " for locale " + locale + " was not found!");
        }
    }

    /**
     * Message formatter for an internationalised message.
     * @param messageKey the message key
     * @return the resolved message text
     */
    public static String msg(String messageKey)
    {
        return getMessage(messageKey, null);
    }

    /**
     * Message formatter with one argument passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param arg the long argument
     * @return the resolved message text
     */
    public static String msg(String messageKey, long arg)
    {
        Object[] args = {String.valueOf(arg)};
        return getMessage(messageKey, args);
    }

    /**
     * Message formatter with arguments passed in that will be embedded in an internationalised message.
     * @param messageKey the message key
     * @param args the arguments
     * @return the resolved message text
     */
    public static String msg(String messageKey, Object... args)
    {
        return getMessage(messageKey, args);
    }

    /**
     * Method to provide the MessageFormat logic. All of the msg()
     * methods are wrappers to this one.
     * @param messageKey the message key
     * @param msgArgs an array of arguments to substitute into the message
     * @return the resolved message text
     */
    private static final String getMessage(String messageKey, Object... msgArgs) 
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
                else if (Throwable.class.isAssignableFrom(msgArgs[i].getClass()))
                { 
                    msgArgs[i] = getStringFromException((Throwable)msgArgs[i]);
                }
            }
        }

        String stringForKey = properties.get(messageKey);
        if (stringForKey == null)
        {
            NucleusLogger.GENERAL.error("Message \"" + messageKey + "\" doesn't exist in any registered ResourceBundle");
            return null;
        }

        if (displayCodesInMessages)
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
            // Format the message with the supplied arguments embedded
            MessageFormat formatter = msgFormats.get(stringForKey);
            if (formatter == null)
            {
                formatter = new MessageFormat(stringForKey);
                msgFormats.put(stringForKey,formatter);
            }
            return formatter.format(msgArgs);
        }
        return stringForKey;
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
            // TODO Add more exceptions here, so we can provide complete information in the log
        }
        return msg.toString();
    }
}