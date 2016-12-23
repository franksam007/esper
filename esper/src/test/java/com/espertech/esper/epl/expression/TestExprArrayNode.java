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

package com.espertech.esper.epl.expression;

import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.ops.ExprArrayNode;
import com.espertech.esper.util.support.SupportExprValidationContextFactory;
import junit.framework.TestCase;
import com.espertech.esper.supportunit.epl.SupportExprNode;

public class TestExprArrayNode extends TestCase
{
    private ExprArrayNode[] arrayNodes;

    public void setUp() throws Exception
    {
        arrayNodes = new ExprArrayNode[4];
        arrayNodes[0] = new ExprArrayNode();

        // no coercion array
        arrayNodes[1] = new ExprArrayNode();
        arrayNodes[1].addChildNode(new SupportExprNode(2));
        arrayNodes[1].addChildNode(new SupportExprNode(3));

        // coercion
        arrayNodes[2] = new ExprArrayNode();
        arrayNodes[2].addChildNode(new SupportExprNode(1.5D));
        arrayNodes[2].addChildNode(new SupportExprNode(1));

        // mixed types
        arrayNodes[3] = new ExprArrayNode();
        arrayNodes[3].addChildNode(new SupportExprNode("a"));
        arrayNodes[3].addChildNode(new SupportExprNode(1));

        for (int i = 0; i < arrayNodes.length; i++)
        {
            arrayNodes[i].validate(SupportExprValidationContextFactory.makeEmpty());
        }
    }

    public void testGetType() throws Exception
    {
        assertEquals(Object[].class, arrayNodes[0].getType());
        assertEquals(Integer[].class, arrayNodes[1].getType());
        assertEquals(Double[].class, arrayNodes[2].getType());
        assertEquals(Object[].class, arrayNodes[3].getType());
    }

    public void testEvaluate() throws Exception
    {
        Object result = arrayNodes[0].evaluate(null, true, null);
        assertEquals(Object[].class, result.getClass());
        assertEquals(0, ((Object[]) result).length);

        result = arrayNodes[1].evaluate(null, true, null);
        assertEquals(Integer[].class, result.getClass());
        assertEquals(2, ((Integer[]) result).length);
        assertEquals(2, (int) ((Integer[]) result)[0]);
        assertEquals(3, (int) ((Integer[]) result)[1]);

        result = arrayNodes[2].evaluate(null, true, null);
        assertEquals(Double[].class, result.getClass());
        assertEquals(2, ((Double[]) result).length);
        assertEquals(1.5, (double) ((Double[]) result)[0]);
        assertEquals(1.0, (double) ((Double[]) result)[1]);

        result = arrayNodes[3].evaluate(null, true, null);
        assertEquals(Object[].class, result.getClass());
        assertEquals(2, ((Object[]) result).length);
        assertEquals("a", ((Object[]) result)[0]);
        assertEquals(1, ((Object[]) result)[1]);
    }

    public void testToExpressionString() throws Exception
    {
        assertEquals("{}", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(arrayNodes[0]));
        assertEquals("{2,3}", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(arrayNodes[1]));
        assertEquals("{1.5,1}", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(arrayNodes[2]));
        assertEquals("{\"a\",1}", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(arrayNodes[3]));
    }

    public void testEqualsNode()
    {
        assertTrue(arrayNodes[0].equalsNode(arrayNodes[1]));
        assertFalse(arrayNodes[0].equalsNode(new SupportExprNode(null)));
    }
}
