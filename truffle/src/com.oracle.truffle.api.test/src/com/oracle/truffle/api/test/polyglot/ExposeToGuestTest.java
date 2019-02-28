/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.test.polyglot;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;


public class ExposeToGuestTest {
    @Test
    public void byDefaultOnlyAnnotatedMethodsCanBeAccessed() {
        Context context = Context.create();
        Value readValue = context.eval("sl", ""
                + "function readValue(x) {\n"
                + "  return x.value;\n"
                + "}\n"
                + "function main() {\n"
                + "  return readValue;\n"
                + "}\n"
        );
        Assert.assertEquals(42, readValue.execute(new ExportedValue()).asInt());
        assertPropertyUndefined("PublicValue isn't enough by default", readValue, new PublicValue());
    }

    private void assertPropertyUndefined(String msg, Value readValue, Object value) {
        try {
            readValue.execute(value);
            fail(msg);
        } catch (PolyglotException ex) {
            assertEquals("Undefined property: value", ex.getMessage());
        }
    }

    public static class PublicValue {
        public int value = 42;
    }

    public static class ExportedValue {
        @HostAccess.Export
        public int value = 42;
    }

    @Test
    public void exportingAllPublicIsEasy() {
        Context context = Context.newBuilder().allowHostAccess(HostAccess.PUBLIC).build();
        Value readValue = context.eval("sl", ""
                + "function readValue(x) {\n"
                + "  return x.value;\n"
                + "}\n"
                + "function main() {\n"
                + "  return readValue;\n"
                + "}\n"
        );
        Assert.assertEquals(42, readValue.execute(new PublicValue()).asInt());
        Assert.assertEquals(42, readValue.execute(new ExportedValue()).asInt());
    }

    @Test
    public void customExportedAnnotation() {
        HostAccess accessMeConfig = HostAccess.newBuilder().allowAccessAnnotatedBy(AccessMe.class).build();
        Context context = Context.newBuilder().allowHostAccess(accessMeConfig).build();
        Value readValue = context.eval("sl", ""
                + "function readValue(x) {\n"
                + "  return x.value;\n"
                + "}\n"
                + "function main() {\n"
                + "  return readValue;\n"
                + "}\n"
        );
        Assert.assertEquals(42, readValue.execute(new AccessibleValue()).asInt());
        assertPropertyUndefined("Default annotation isn't enough", readValue, new ExportedValue());
        assertPropertyUndefined("Public isn't enough by default", readValue, new PublicValue());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface AccessMe {
    }

    public static class AccessibleValue {
        @AccessMe
        public int value = 42;
    }

    @Test
    public void explicitlyEnumeratingField() throws Exception {
        HostAccess explictConfig = HostAccess.newBuilder().
                allowAccess(AccessibleValue.class.getField("value")).
                build();
        Context context = Context.newBuilder().allowHostAccess(explictConfig).build();
        Value readValue = context.eval("sl", ""
                + "function readValue(x) {\n"
                + "  return x.value;\n"
                + "}\n"
                + "function main() {\n"
                + "  return readValue;\n"
                + "}\n"
        );
        Assert.assertEquals(42, readValue.execute(new AccessibleValue()).asInt());
        assertPropertyUndefined("Default annotation isn't enough", readValue, new ExportedValue());
        assertPropertyUndefined("Public isn't enough by default", readValue, new PublicValue());
    }

}
