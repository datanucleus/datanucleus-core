package org.datanucleus.plugin;

import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.Bundle;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.Extension;
import org.datanucleus.plugin.ExtensionPoint;
import org.datanucleus.plugin.NonManagedPluginRegistry;
import org.datanucleus.plugin.PluginParser;

public class PluginParserTest extends TestCase
{
    public void testParseExtensionPoint()
    {
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();
        NonManagedPluginRegistry mgr = new NonManagedPluginRegistry(clr, "EXCEPTION", true);
        assertEquals(0,mgr.getExtensionPoints().length);

        Bundle bundle0 = mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST0.MF",null)); 
        mgr.registerExtensionsForPlugin(clr.getResource("/org/datanucleus/samples/plugin/plugin1expoint.xml",null),bundle0);
        assertEquals(2,mgr.getExtensionPoints().length);
        assertNull(mgr.getExtensionPoint("testID"));
        assertNull(mgr.getExtensionPoint("testID2"));
        assertNotNull(mgr.getExtensionPoint("org.datanucleus.testID"));
        assertNotNull(mgr.getExtensionPoint("org.datanucleus.testID2"));
        ExtensionPoint point = mgr.getExtensionPoint("org.datanucleus.testID");
        assertEquals("testID", point.getId());
        assertEquals("org.datanucleus.testID", point.getUniqueId());
        assertEquals("testName", point.getName());
        assertNotNull(clr.getResource("/org/datanucleus/samples/plugin/plugin1.xsd",null));
        assertEquals(clr.getResource("/org/datanucleus/samples/plugin/plugin1.xsd",null), point.getSchema());
        assertEquals(0,point.getExtensions().length);

        mgr.registerExtensionsForPlugin(clr.getResource("/org/datanucleus/samples/plugin/plugin1.xml",null),bundle0);
        assertEquals(2,point.getExtensions().length);        
        Extension[] exts = point.getExtensions();
        assertEquals(exts[0].getPlugin(),exts[1].getPlugin());
        assertEquals(2,exts[0].getConfigurationElements().length);
        
        ConfigurationElement[] level1 = exts[0].getConfigurationElements();
        assertEquals(2,level1[0].getChildren().length);
       
        assertEquals("level1",level1[0].getName());
        assertEquals(1,level1[0].getAttributeNames().length);
        assertEquals("1",level1[0].getAttribute("attr11"));
        assertNull(level1[0].getAttribute("attr11XXX"));
        
        ConfigurationElement[] level2 = level1[0].getChildren();
        assertEquals(1,level2[0].getChildren().length);
        assertEquals("level2",level2[0].getName());
        assertEquals(2,level2[0].getAttributeNames().length);
        assertEquals("attr21",level2[0].getAttributeNames()[0]);
        assertEquals("attr22",level2[0].getAttributeNames()[1]);
        assertEquals("2211",level2[0].getAttribute("attr21"));
        assertEquals("2221",level2[0].getAttribute("attr22"));
        assertNull(level2[0].getAttribute("attr11XXX"));

        assertEquals(0,level1[1].getChildren().length);
        assertEquals("2",level1[1].getAttribute("attr11"));

        assertEquals(1,exts[1].getConfigurationElements().length);
        level1 = exts[1].getConfigurationElements();
        assertEquals("A",level1[0].getAttribute("attr11"));
        assertEquals(0,level1[0].getChildren().length);
    }
    
    public void testParseSymbolicName()
    {
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();
        NonManagedPluginRegistry mgr = new NonManagedPluginRegistry(clr, "EXCEPTION", true);

        Bundle bundle1 = mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST1.MF",null)); 
        mgr.registerExtensionsForPlugin(clr.getResource("/org/datanucleus/samples/plugin/plugin1expoint.xml",null),bundle1);
        assertEquals(2,mgr.getExtensionPoints().length);
        assertNull(mgr.getExtensionPoint("testID"));
        assertNull(mgr.getExtensionPoint("testID2"));
        assertNotNull(mgr.getExtensionPoint("org.datanucleus.plugin.test1.testID"));
        assertNotNull(mgr.getExtensionPoint("org.datanucleus.plugin.test1.testID2"));

        Bundle bundle2 = mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST2.MF",null)); 
        mgr.registerExtensionsForPlugin(clr.getResource("/org/datanucleus/samples/plugin/plugin1expoint.xml",null),bundle2);
        assertEquals(4,mgr.getExtensionPoints().length);
        assertNotNull(mgr.getExtensionPoint("org.datanucleus.plugin.test2.testID"));
        assertNotNull(mgr.getExtensionPoint("org.datanucleus.plugin.test2.testID2"));

        Extension[] ex = mgr.getExtensionPoint("org.datanucleus.plugin.test2.testID").getExtensions();
        assertEquals(ex.length, 0);
        /*assertEquals("org.datanucleus.plugin.test2",ex[ex.length-1].getPlugin().getSymbolicName());*/
    }

    /**
     * NonManagedPluginRegistry cannot handle multiple versions
     * of the same plugin, so it must raise an exception
     */
    public void testDuplicatedBundleSymbolicNameSameOrDifferentVersion()
    {
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();
        NonManagedPluginRegistry mgr = new NonManagedPluginRegistry(clr, "EXCEPTION", true);
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST1.MF",null)); 
        try
        {
            mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST1-1.MF",null)); 
        	fail("Expected JPOXException");
        }
        catch(NucleusException ex)
        {
        	//expected
        }
    }
    
    public void testParser()
    {
        PluginParser.Parser p1 = new PluginParser.Parser("org.datanucleus,org.datanucleus.enhancer");
        assertEquals("org.datanucleus",p1.parseSymbolicName());
        assertEquals(0,p1.parseParameters().size());
        assertEquals("org.datanucleus.enhancer",p1.parseSymbolicName());
        assertNull(p1.parseSymbolicName());
        assertEquals(0,p1.parseParameters().size());

        PluginParser.Parser p2 = new PluginParser.Parser("org.datanucleus;value=arg;value2=arg2;value3:=\"arg3\",org.datanucleus.enhancer,org.datanucleus.test;test=e,org.datanucleus.test2");
        assertEquals("org.datanucleus",p2.parseSymbolicName());
        Map p = p2.parseParameters();
        assertEquals(3,p.size());
        assertEquals("arg",p.get("value"));
        assertEquals("arg3",p.get("value3"));
        assertEquals("arg2",p.get("value2"));
        assertEquals("org.datanucleus.enhancer",p2.parseSymbolicName());
        assertEquals("org.datanucleus.test",p2.parseSymbolicName());
        p = p2.parseParameters();
        assertEquals(1,p.size());
        assertEquals("e",p.get("test"));
        assertEquals("org.datanucleus.test2",p2.parseSymbolicName());
    }

    public void testParserInvalidCharsIgnored()
    {
        PluginParser.Parser p1 = new PluginParser.Parser("org.datanucleus,org.datanucleus.enhancer,org.datanucleus.test**,org.datanucleus.tt");
        assertEquals("org.datanucleus",p1.parseSymbolicName());
        assertEquals("org.datanucleus.enhancer",p1.parseSymbolicName());
        assertEquals("org.datanucleus.test",p1.parseSymbolicName());
        try
        {
            p1.parseSymbolicName();
            fail("expected exception");
        }
        catch(NucleusUserException e){}
    }

    public void testRequireBundle()
    {
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();
        NonManagedPluginRegistry mgr = new NonManagedPluginRegistry(clr, "EXCEPTION", true);
        assertEquals(0,mgr.getExtensionPoints().length);
        Bundle bundle3 = mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST3.MF",null)); 
        Bundle bundle4 = mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST4.MF",null)); 
        Bundle bundle5 = mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST5.MF",null));
        assertEquals(1,bundle3.getRequireBundle().size());
        assertEquals("org.datanucleus.plugin.test4",(bundle3.getRequireBundle().iterator().next()).getBundleSymbolicName());
        assertEquals(0,bundle4.getRequireBundle().size());
        assertEquals(2,bundle5.getRequireBundle().size());
        assertEquals("org.datanucleus.plugin.test6",(bundle5.getRequireBundle().get(0)).getBundleSymbolicName());
        assertEquals("org.datanucleus.plugin.test7",(bundle5.getRequireBundle().get(1)).getBundleSymbolicName());
        assertEquals("org.datanucleus.plugin.test7",(bundle5.getRequireBundle().get(1)).getBundleSymbolicName());
        assertEquals("optional",(bundle5.getRequireBundle().get(1)).getParameter("resolution"));
    }

    public void testRequireBundleLogged()
    {
        final java.util.Set messages = new HashSet();
        Logger.getLogger("DataNucleus.General").addAppender(new Appender()
        {
            public void setName(String arg0)
            {
            }
        
            public void setLayout(Layout arg0)
            {
            }
        
            public void setErrorHandler(ErrorHandler arg0)
            {
            }
        
            public boolean requiresLayout()
            {
                return false;
            }
        
            public String getName()
            {
                return "testappender123";
            }
        
            public Layout getLayout()
            {
                return null;
            }
        
            public Filter getFilter()
            {
                return null;
            }
        
            public ErrorHandler getErrorHandler()
            {
                return null;
            }
        
            public void doAppend(LoggingEvent arg0)
            {
                if (arg0.getRenderedMessage().indexOf("but it cannot be resolved") > 0)
                {
                    messages.add(arg0.getRenderedMessage());
                }
            }
        
            public void close(){}
            public void clearFilters(){}
            public void addFilter(Filter arg0){}
        
        });
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();
        NonManagedPluginRegistry mgr = new NonManagedPluginRegistry(clr, "EXCEPTION", true);
        assertEquals(0,mgr.getExtensionPoints().length);
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST3.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST4.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST5.MF",null));
        mgr.resolveConstraints();
        try
        {
            assertEquals(2,messages.size());
            assertTrue(messages.contains("Bundle \"org.datanucleus.plugin.test5\" requires \"org.datanucleus.plugin.test6\" but it cannot be resolved."));
            assertTrue(messages.contains("Bundle \"org.datanucleus.plugin.test5\" has an optional dependency to \"org.datanucleus.plugin.test7\" but it cannot be resolved"));
        }
        finally
        {
            Logger.getLogger("DataNucleus.General").removeAppender("testappender123");
        }
    }

    public void testRequireBundleVersionLogged()
    {
        final java.util.Set messages = new HashSet();
        Logger.getLogger("DataNucleus.General").addAppender(new Appender()
        {
            public void setName(String arg0)
            {
            }
        
            public void setLayout(Layout arg0)
            {
            }
        
            public void setErrorHandler(ErrorHandler arg0)
            {
            }
        
            public boolean requiresLayout()
            {
                return false;
            }
        
            public String getName()
            {
                return "testappender123";
            }
        
            public Layout getLayout()
            {
                return null;
            }
        
            public Filter getFilter()
            {
                return null;
            }
        
            public ErrorHandler getErrorHandler()
            {
                return null;
            }
        
            public void doAppend(LoggingEvent arg0)
            {
                if (arg0.getLevel() != Level.DEBUG)
                {
                    messages.add(arg0.getRenderedMessage());
                }
            }
        
            public void close(){}
            public void clearFilters(){}
            public void addFilter(Filter arg0){}
        });
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();
        NonManagedPluginRegistry mgr = new NonManagedPluginRegistry(clr, "EXCEPTION", true);
        assertEquals(0,mgr.getExtensionPoints().length);
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST10.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST11.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST12.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST13.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST14.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST15.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST16.MF",null)); 
        mgr.registerBundle(clr.getResource("/org/datanucleus/samples/plugin/MANIFEST17.MF",null)); 
        mgr.resolveConstraints();
        try
        {
            assertEquals(3,messages.size());
            assertTrue(messages.contains("Bundle \"org.datanucleus.plugin.test12\" requires \"org.datanucleus.plugin.test11\" version \"(1.2.0.b2\" but the resolved bundle has version \"1.2.0.b2\" which is outside the expected range."));
            assertTrue(messages.contains("Bundle \"org.datanucleus.plugin.test13\" requires \"org.datanucleus.plugin.test11\" version \"(1.2.0.c1\" but the resolved bundle has version \"1.2.0.b2\" which is outside the expected range."));
            assertTrue(messages.contains("Bundle \"org.datanucleus.plugin.test15\" requires \"org.datanucleus.plugin.test11\" version \"(1.0.0.b2,1.2.0.b2)\" but the resolved bundle has version \"1.2.0.b2\" which is outside the expected range."));
        }
        finally
        {
            Logger.getLogger("DataNucleus.General").removeAppender("testappender123");
        }
    }
}