/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
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
2008 Andy Jefferson - support arguments. Support for quick return for non-transformable classes
    ...
**********************************************************************/
package org.datanucleus.enhancer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;

import org.datanucleus.util.CommandLine;

/**
 * Entry Point (as per Java) for transforming classes at runtime.
 * Before loading classes, the JVM invokes the ClassFileTranformer to transform the class.
 * Will never process classes in packages "java.", "javax.", "org.datanucleus." (when not "test" or "samples").
 * Accepts the following (optional) arguments
 * <ul>
 * <li>api : JDO, JPA - default=JDO</li>
 * <li>generatePK : true, false - default=true</li>
 * <li>generateConstructor : true, false - default=true</li>
 * <li>detachListener : true, false - default=false</li>
 * <li>default args : package names of classes to be enhanced when encountered
 * </ul>
 */
public class DataNucleusClassFileTransformer implements ClassFileTransformer
{
    protected RuntimeEnhancer enhancer;

    /** User input package name(s) (comma-separated) that should be processed. */
    private CommandLine cmd = new CommandLine();

    public DataNucleusClassFileTransformer(String arguments, Map contextProps)
    {
        cmd.addOption("api", "api", "api", "api");
        cmd.addOption("generatePK", "generatePK", "<generate-pk>", "Generate PK class where needed?");
        cmd.addOption("generateConstructor", "generateConstructor", "<generate-constructor>", "Generate default constructor where needed?");
        cmd.addOption("detachListener", "detachListener", "<detach-listener>", "Use Detach Listener?");
        if (arguments != null)
        {
            cmd.parse(arguments.split("[\\s,=]+"));
        }

        String api = cmd.getOptionArg("api") != null ? cmd.getOptionArg("api") : null;
        if (api == null)
        {
        	api = "JDO";
        	DataNucleusEnhancer.LOGGER.debug("ClassFileTransformer API not specified so falling back to JDO. You should specify '-api={API}' when specifying the javaagent");
        }

        // Create the RuntimeEnhancer with the specified API and options/properties
        enhancer = new RuntimeEnhancer(api, contextProps);
        if (cmd.hasOption("generateConstructor"))
        {
            String val = cmd.getOptionArg("generateConstructor");
            if (val.equalsIgnoreCase("false"))
            {
                enhancer.unsetClassEnhancerOption(ClassEnhancer.OPTION_GENERATE_DEFAULT_CONSTRUCTOR);
            }
        }
        if (cmd.hasOption("generatePK"))
        {
            String val = cmd.getOptionArg("generatePK");
            if (val.equalsIgnoreCase("false"))
            {
                enhancer.unsetClassEnhancerOption(ClassEnhancer.OPTION_GENERATE_PK);
            }
        }
        if (cmd.hasOption("detachListener"))
        {
            String val = cmd.getOptionArg("detachListener");
            if (val.equalsIgnoreCase("true"))
            {
                enhancer.setClassEnhancerOption(ClassEnhancer.OPTION_GENERATE_DETACH_LISTENER);
            }
        }
    }

    public static void premain(String agentArguments, Instrumentation instrumentation)
    {
    	// Called when invoking the JRE with javaagent, at startup
        instrumentation.addTransformer(new DataNucleusClassFileTransformer(agentArguments, null));
    } 

    /**
     * Invoked when a class is being loaded or redefined. The implementation of
     * this method may transform the supplied class file and return a new replacement class file.
     * @param loader The defining loader of the class to be transformed, may be null if the bootstrap loader
     * @param className The name of the class in the internal form of fully qualified class and interface names
     * @param classBeingRedefined If this is a redefine, the class being redefined, otherwise null
     * @param protectionDomain The protection domain of the class being defined or redefined
     * @param classfileBuffer The input byte buffer in class file format - must not be modified
     * @return A well-formed class file buffer (the result of the transform), or null if no transform is performed
     * @throws IllegalClassFormatException If the input does not represent a well-formed class file
     */
    public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined, 
            ProtectionDomain protectionDomain, byte[] classfileBuffer)
        throws IllegalClassFormatException
    {
        String name = className.replace('/','.');
        if (name.startsWith("java."))
        {
            return null;
        }
        else if (name.startsWith("javax."))
        {
            return null;
        }
        else if (name.startsWith("org.datanucleus."))
        {
            // Only allow "org.datanucleus.samples"/"org.datanucleus.test" through to transformer
            if (!name.startsWith("org.datanucleus.samples") && !name.startsWith("org.datanucleus.test"))
            {
                return null;
            }
        }

        if (cmd.getDefaultArgs() != null && cmd.getDefaultArgs().length>0)
        {
            // Arguments is comma-separated set of package names "to-be-processed"
            String[] classes = cmd.getDefaultArgs();
            for (int i=0; i<classes.length; i++)
            {
                if (name.startsWith(classes[i]))
                {
                    // Class in a package to be process so transform it
                    return enhancer.enhance(name, classfileBuffer, loader);
                }
            }

            return null;
        }
        else
        {
            // No args provided so pass all through to the transformer
            return enhancer.enhance(name, classfileBuffer, loader);
        }
    }
}