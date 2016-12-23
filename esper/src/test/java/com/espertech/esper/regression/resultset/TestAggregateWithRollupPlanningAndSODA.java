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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.annotation.HookType;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPlanDesc;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportGroupRollupPlanHook;
import junit.framework.TestCase;

import java.io.StringWriter;

public class TestAggregateWithRollupPlanningAndSODA extends TestCase
{
    public final static String PLAN_CALLBACK_HOOK = "@Hook(type=" + HookType.class.getName() + ".INTERNAL_GROUPROLLUP_PLAN,hook='" + SupportGroupRollupPlanHook.class.getName() + "')";

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testGroupByPlanning() {
        epService.getEPAdministrator().getConfiguration().addEventType(ABCProp.class);

        // plain rollup
        validate("a", "rollup(a)", new String[]{"a", ""});
        validate("a, b", "rollup(a, b)", new String[]{"a,b", "a", ""});
        validate("a, b, c", "rollup(a, b, c)", new String[]{"a,b,c", "a,b", "a", ""});
        validate("a, b, c, d", "rollup(a, b, c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b", "a", ""});

        // rollup with unenclosed
        validate("a, b", "a, rollup(b)", new String[]{"a,b", "a"});
        validate("a, b, c", "a, b, rollup(c)", new String[]{"a,b,c", "a,b"});
        validate("a, b, c", "a, rollup(b, c)", new String[]{"a,b,c", "a,b", "a"});
        validate("a, b, c, d", "a, b, rollup(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b"});
        validate("a, b, c, d, e", "a, b, rollup(c, d, e)", new String[]{"a,b,c,d,e", "a,b,c,d", "a,b,c", "a,b"});

        // plain cube
        validate("a", "cube(a)", new String[]{"a", ""});
        validate("a, b", "cube(a, b)", new String[]{"a,b", "a", "b", ""});
        validate("a, b, c", "cube(a, b, c)", new String[]{"a,b,c", "a,b", "a,c", "a", "b,c", "b", "c", ""});
        validate("a, b, c, d", "cube(a, b, c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b,d",
                "a,b", "a,c,d", "a,c", "a,d", "a",
                "b,c,d", "b,c", "b,d", "b",
                "c,d", "c", "d", ""});

        // cube with unenclosed
        validate("a, b", "a, cube(b)", new String[]{"a,b", "a"});
        validate("a, b, c", "a, cube(b, c)", new String[]{"a,b,c", "a,b", "a,c", "a"});
        validate("a, b, c, d", "a, cube(b, c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b,d", "a,b", "a,c,d", "a,c", "a,d", "a"});
        validate("a, b, c, d", "a, b, cube(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b,d", "a,b"});

        // plain grouping set
        validate("a", "grouping sets(a)", new String[]{"a"});
        validate("a", "grouping sets(a)", new String[]{"a"});
        validate("a, b", "grouping sets(a, b)", new String[]{"a", "b"});
        validate("a, b", "grouping sets(a, b, (a, b), ())", new String[]{"a", "b", "a,b", ""});
        validate("a, b", "grouping sets(a, (a, b), (), b)", new String[]{"a", "a,b", "", "b"});
        validate("a, b, c", "grouping sets((a, b), (a, c), (), (b, c))", new String[]{"a,b", "a,c", "", "b,c"});
        validate("a, b", "grouping sets((a, b))", new String[]{"a,b"});
        validate("a, b, c", "grouping sets((a, b, c), ())", new String[]{"a,b,c", ""});
        validate("a, b, c", "grouping sets((), (a, b, c), (b, c))", new String[]{"", "a,b,c", "b,c"});

        // grouping sets with unenclosed
        validate("a, b", "a, grouping sets(b)", new String[]{"a,b"});
        validate("a, b, c", "a, grouping sets(b, c)", new String[]{"a,b", "a,c"});
        validate("a, b, c", "a, grouping sets((b, c))", new String[]{"a,b,c"});
        validate("a, b, c, d", "a, b, grouping sets((), c, d, (c, d))", new String[]{"a,b", "a,b,c", "a,b,d", "a,b,c,d"});

        // multiple grouping sets
        validate("a, b", "grouping sets(a), grouping sets(b)", new String[]{"a,b"});
        validate("a, b, c", "grouping sets(a), grouping sets(b, c)", new String[]{"a,b", "a,c"});
        validate("a, b, c, d", "grouping sets(a, b), grouping sets(c, d)", new String[]{"a,c", "a,d", "b,c", "b,d"});
        validate("a, b, c", "grouping sets((), a), grouping sets(b, c)", new String[]{"b", "c", "a,b", "a,c"});
        validate("a, b, c, d", "grouping sets(a, b, c), grouping sets(d)", new String[]{"a,d", "b,d", "c,d"});
        validate("a, b, c, d, e", "grouping sets(a, b, c), grouping sets(d, e)", new String[]{"a,d", "a,e", "b,d", "b,e", "c,d", "c,e"});

        // multiple rollups
        validate("a, b, c", "rollup(a, b), rollup(c)", new String[]{"a,b,c", "a,b", "a,c", "a", "c", ""});
        validate("a, b, c, d", "rollup(a, b), rollup(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b", "a,c,d", "a,c", "a", "c,d", "c", ""});

        // grouping sets with rollup or cube inside
        validate("a, b, c", "grouping sets(a, rollup(b, c))", new String[]{"a", "b,c", "b", ""});
        validate("a, b, c", "grouping sets(a, cube(b, c))", new String[]{"a", "b,c", "b", "c", ""});
        validate("a, b", "grouping sets(rollup(a, b))", new String[]{"a,b", "a", ""});
        validate("a, b", "grouping sets(cube(a, b))", new String[]{"a,b", "a", "b", ""});
        validate("a, b, c, d", "grouping sets((a, b), rollup(c, d))", new String[]{"a,b", "c,d", "c", ""});
        validate("a, b, c, d", "grouping sets(a, b, rollup(c, d))", new String[]{"a", "b", "c,d", "c", ""});

        // cube and rollup with combined expression
        validate("a, b, c", "cube((a, b), c)", new String[]{"a,b,c", "a,b", "c", ""});
        validate("a, b, c", "rollup((a, b), c)", new String[]{"a,b,c", "a,b", ""});
        validate("a, b, c, d", "cube((a, b), (c, d))", new String[]{"a,b,c,d", "a,b", "c,d", ""});
        validate("a, b, c, d", "rollup((a, b), (c, d))", new String[]{"a,b,c,d", "a,b", ""});
        validate("a, b, c", "cube(a, (b, c))", new String[]{"a,b,c", "a", "b,c", ""});
        validate("a, b, c", "rollup(a, (b, c))", new String[]{"a,b,c", "a", ""});
        validate("a, b, c", "grouping sets(rollup((a, b), c))", new String[]{"a,b,c", "a,b", ""});

        // multiple cubes and rollups
        validate("a, b, c, d", "rollup(a, b), rollup(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b",
                "a,c,d", "a,c", "a", "c,d", "c", ""});
        validate("a, b", "cube(a), cube(b)", new String[]{"a,b", "a", "b", ""});
        validate("a, b, c", "cube(a, b), cube(c)", new String[]{"a,b,c", "a,b", "a,c", "a", "b,c", "b", "c", ""});
    }

    private void validate(String selectClause, String groupByClause, String[] expectedCSV) {

        String epl = PLAN_CALLBACK_HOOK + " select " + selectClause + ", count(*) from ABCProp group by " + groupByClause;
        SupportGroupRollupPlanHook.reset();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        comparePlan(expectedCSV);
        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        SupportGroupRollupPlanHook.reset();
        stmt = epService.getEPAdministrator().create(model);
        comparePlan(expectedCSV);
        assertEquals(epl, stmt.getText());
        stmt.destroy();
    }

    private void comparePlan(String[] expectedCSV) {
        GroupByRollupPlanDesc plan = SupportGroupRollupPlanHook.getPlan();
        AggregationGroupByRollupLevel[] levels = plan.getRollupDesc().getLevels();
        String[][] received = new String[levels.length][];
        for (int i = 0; i < levels.length; i++) {
            AggregationGroupByRollupLevel level = levels[i];
            if (level.isAggregationTop()) {
                received[i] = new String[0];
            }
            else {
                received[i] = new String[level.getRollupKeys().length];
                for (int j = 0; j < received[i].length; j++) {
                    int key = level.getRollupKeys()[j];
                    received[i][j] = ExprNodeUtility.toExpressionStringMinPrecedenceSafe(plan.getExpressions()[key]);;
                }
            }
        }

        assertEquals("Received: " + toCSV(received), expectedCSV.length, received.length);
        for (int i = 0; i < expectedCSV.length; i++) {
            String receivedCSV = toCSV(received[i]);
            assertEquals("Failed at row " + i, expectedCSV[i], receivedCSV);
        }
    }

    private String toCSV(String[][] received) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (String[] item : received) {
            writer.append(delimiter);
            writer.append(toCSV(item));
            delimiter = "  ";
        }
        return writer.toString();
    }

    private String toCSV(String[] received) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (String item : received) {
            writer.append(delimiter);
            writer.append(item);
            delimiter = ",";
        }
        return writer.toString();
    }

    private static class ABCProp {
        private final String a;
        private final String b;
        private final String c;
        private final String d;
        private final String e;
        private final String f;
        private final String g;
        private final String h;

        private ABCProp(String a, String b, String c, String d, String e, String f, String g, String h) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
            this.g = g;
            this.h = h;
        }

        public String getA() {
            return a;
        }

        public String getB() {
            return b;
        }

        public String getC() {
            return c;
        }

        public String getD() {
            return d;
        }

        public String getE() {
            return e;
        }

        public String getF() {
            return f;
        }

        public String getG() {
            return g;
        }

        public String getH() {
            return h;
        }
    }
}
