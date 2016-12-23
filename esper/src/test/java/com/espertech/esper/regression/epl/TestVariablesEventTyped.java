/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestVariablesEventTyped extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private SupportUpdateListener listenerSet;

    protected void tearDown() throws Exception {
        listener = null;
        listenerSet = null;
    }

    public void testInvalid() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("TypeS0", SupportBean_S0.class);
        config.addVariable("vars0", "TypeS0", null);
        config.addVariable("vars1", SupportBean_S1.class.getName(), null);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        try {
            epService.getEPRuntime().setVariableValue("vars0", new SupportBean_S1(1));
            fail();
        }
        catch (VariableValueException ex) {
            assertEquals("Variable 'vars0' of declared event type 'TypeS0' underlying type '" + SupportBean_S0.class.getName() + "' cannot be assigned a value of type '" + SupportBean_S1.class.getName() + "'", ex.getMessage());
        }
        
        tryInvalid(epService, "on TypeS0 arrival set vars1 = arrival",
                   "Error starting statement: Error in variable assignment: Variable 'vars1' of declared event type '" + SupportBean_S1.class.getName() + "' underlying type '" + SupportBean_S1.class.getName() + "' cannot be assigned a value of type '" + SupportBean_S0.class.getName() + "'");

        tryInvalid(epService, "on TypeS0 arrival set vars0 = 1",
                   "Error starting statement: Error in variable assignment: Variable 'vars0' of declared event type 'TypeS0' underlying type '" + SupportBean_S0.class.getName() + "' cannot be assigned a value of type 'java.lang.Integer' [on TypeS0 arrival set vars0 = 1]");
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testConfig() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("TypeS0", SupportBean_S0.class);
        config.addEventType("TypeS2", SupportBean_S2.class);

        config.addVariable("vars0", "TypeS0", new SupportBean_S0(10));
        config.addVariable("vars1", SupportBean_S1.class.getName(), new SupportBean_S1(20));
        config.addVariable("varsobj1", Object.class.getName(), 123);

        NonSerializable nonSerializable = new NonSerializable("abc");
        config.addVariable("myNonSerializable", NonSerializable.class, nonSerializable);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        assertEquals(10, ((SupportBean_S0) epService.getEPRuntime().getVariableValue("vars0")).getId());
        assertEquals(20, ((SupportBean_S1) epService.getEPRuntime().getVariableValue("vars1")).getId());
        assertEquals(123, epService.getEPRuntime().getVariableValue("varsobj1"));
        assertSame(nonSerializable, epService.getEPRuntime().getVariableValue("myNonSerializable"));

        epService.getEPAdministrator().getConfiguration().addVariable("vars2", "TypeS2", new SupportBean_S2(30));
        epService.getEPAdministrator().getConfiguration().addVariable("vars3", SupportBean_S3.class, new SupportBean_S3(40));
        epService.getEPAdministrator().getConfiguration().addVariable("varsobj2", Object.class, "ABC");

        assertEquals(30, ((SupportBean_S2) epService.getEPRuntime().getVariableValue("vars2")).getId());
        assertEquals(40, ((SupportBean_S3) epService.getEPRuntime().getVariableValue("vars3")).getId());
        assertEquals("ABC", epService.getEPRuntime().getVariableValue("varsobj2"));
        
        epService.getEPAdministrator().createEPL("create variable object varsobj3=222");
        assertEquals(222, epService.getEPRuntime().getVariableValue("varsobj3"));
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testEventTypedSetProp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
        listenerSet = new SupportUpdateListener();

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().createEPL("create variable SupportBean varbean");

        String[] fields = "varbean.theString,varbean.intPrimitive,varbean.getTheString()".split(",");
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select varbean.theString,varbean.intPrimitive,varbean.getTheString() from S0");
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

        EPStatement stmtSet = epService.getEPAdministrator().createEPL("on A set varbean.theString = 'A', varbean.intPrimitive = 1");
        stmtSet.addListener(listenerSet);
        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        listenerSet.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

        SupportBean setBean = new SupportBean();
        epService.getEPRuntime().setVariableValue("varbean", setBean);
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 1, "A"});
        assertNotSame(setBean, epService.getEPRuntime().getVariableValue("varbean"));
        assertEquals(1, ((SupportBean) epService.getEPRuntime().getVariableValue("varbean")).getIntPrimitive());
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), "varbean.theString,varbean.intPrimitive".split(","), new Object[]{"A", 1});
        EPAssertionUtil.assertProps(stmtSet.iterator().next(), "varbean.theString,varbean.intPrimitive".split(","), new Object[]{"A", 1});

        // test self evaluate
        stmtSet.destroy();
        stmtSet = epService.getEPAdministrator().createEPL("on A set varbean.theString = A.id, varbean.theString = '>'||varbean.theString||'<'");
        stmtSet.addListener(listenerSet);
        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));
        assertEquals(">E3<", ((SupportBean) epService.getEPRuntime().getVariableValue("varbean")).getTheString());

        // test widen
        stmtSet.destroy();
        stmtSet = epService.getEPAdministrator().createEPL("on A set varbean.longPrimitive = 1");
        stmtSet.addListener(listenerSet);
        epService.getEPRuntime().sendEvent(new SupportBean_A("E4"));
        assertEquals(1, ((SupportBean) epService.getEPRuntime().getVariableValue("varbean")).getLongPrimitive());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testEventTyped() {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
        listenerSet = new SupportUpdateListener();

        epService.getEPAdministrator().getConfiguration().addEventType("S0Type", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // assign to properties of a variable
        // assign: configuration runtime + config static
        // SODA
        epService.getEPAdministrator().createEPL("create variable Object varobject = null");
        epService.getEPAdministrator().createEPL("create variable " + SupportBean_A.class.getName() + " varbean = null");
        epService.getEPAdministrator().createEPL("create variable S0Type vartype = null");

        String[] fields = "varobject,varbean,varbean.id,vartype,vartype.id".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select varobject, varbean, varbean.id, vartype, vartype.id from SupportBean");
        stmt.addListener(listener);

        // test null
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

        // test objects
        SupportBean_A a1objectOne = new SupportBean_A("A1");
        SupportBean_S0 s0objectOne = new SupportBean_S0(1);
        epService.getEPRuntime().setVariableValue("varobject", "abc");
        epService.getEPRuntime().setVariableValue("varbean", a1objectOne);
        epService.getEPRuntime().setVariableValue("vartype", s0objectOne);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"abc", a1objectOne, a1objectOne.getId(), s0objectOne, s0objectOne.getId()});

        // test on-set for Object and EventType
        String[] fieldsTop = "varobject,vartype,varbean".split(",");
        EPStatement stmtSet = epService.getEPAdministrator().createEPL("on S0Type(p00='X') arrival set varobject=1, vartype=arrival, varbean=null");
        stmtSet.addListener(listener);

        SupportBean_S0 s0objectTwo = new SupportBean_S0(2, "X");
        epService.getEPRuntime().sendEvent(s0objectTwo);
        assertEquals(1, epService.getEPRuntime().getVariableValue("varobject"));
        assertEquals(s0objectTwo, epService.getEPRuntime().getVariableValue("vartype"));
        assertEquals(s0objectTwo, epService.getEPRuntime().getVariableValue(Collections.singleton("vartype")).get("vartype"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTop, new Object[]{1, s0objectTwo, null});
        EPAssertionUtil.assertProps(stmtSet.iterator().next(), fieldsTop, new Object[]{1, s0objectTwo, null});

        // set via API to null
        Map<String,Object> newValues = new HashMap<String, Object>();
        newValues.put("varobject", null);
        newValues.put("vartype", null);
        newValues.put("varbean", null);
        epService.getEPRuntime().setVariableValue(newValues);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

        // set via API to values
        newValues.put("varobject", 10L);
        newValues.put("vartype", s0objectTwo);
        newValues.put("varbean", a1objectOne);
        epService.getEPRuntime().setVariableValue(newValues);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10L, a1objectOne, a1objectOne.getId(), s0objectTwo, s0objectTwo.getId()});

        // test on-set for Bean class
        stmtSet = epService.getEPAdministrator().createEPL("on " + SupportBean_A.class.getName() + "(id='Y') arrival set varobject=null, vartype=null, varbean=arrival");
        stmtSet.addListener(listener);
        SupportBean_A a1objectTwo = new SupportBean_A("Y");
        epService.getEPRuntime().sendEvent(new SupportBean_A("Y"));
        assertEquals(null, epService.getEPRuntime().getVariableValue("varobject"));
        assertEquals(null, epService.getEPRuntime().getVariableValue("vartype"));
        assertEquals(a1objectTwo, epService.getEPRuntime().getVariableValue(Collections.singleton("varbean")).get("varbean"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTop, new Object[]{null, null, a1objectTwo});
        EPAssertionUtil.assertProps(stmtSet.iterator().next(), fieldsTop, new Object[]{null, null, a1objectTwo});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void tryInvalid(EPServiceProvider engine, String epl, String message)
    {
        try {
            engine.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }
    }

    public static class NonSerializable {
        private final String myString;

        public NonSerializable(String myString) {
            this.myString = myString;
        }

        public String getMyString() {
            return myString;
        }
    }
}
