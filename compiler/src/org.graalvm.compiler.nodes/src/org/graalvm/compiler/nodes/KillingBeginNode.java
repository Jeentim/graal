/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.compiler.nodes;

import static org.graalvm.compiler.nodeinfo.InputType.Memory;
import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.memory.SingleMemoryKill;
import org.graalvm.compiler.nodes.spi.Canonicalizable;
import org.graalvm.compiler.nodes.spi.CanonicalizerTool;
import org.graalvm.word.LocationIdentity;

/**
 * A begin node that kills a single memory location. See {@link MultiKillingBeginNode} for a version
 * with multiple killed locations.
 *
 * @see WithExceptionNode for more details
 */
@NodeInfo(allowedUsageTypes = {Memory}, cycles = CYCLES_0, size = SIZE_0)
public final class KillingBeginNode extends AbstractBeginNode implements SingleMemoryKill, Canonicalizable {

    public static final NodeClass<KillingBeginNode> TYPE = NodeClass.create(KillingBeginNode.class);
    protected LocationIdentity locationIdentity;

    public KillingBeginNode(LocationIdentity locationIdentity) {
        super(TYPE);
        this.locationIdentity = locationIdentity;
    }

    public static AbstractBeginNode begin(FixedNode with, LocationIdentity locationIdentity) {
        if (with instanceof KillingBeginNode) {
            return (KillingBeginNode) with;
        }
        AbstractBeginNode begin = with.graph().add(KillingBeginNode.create(locationIdentity));
        begin.setNext(with);
        return begin;
    }

    public static AbstractBeginNode create(LocationIdentity locationIdentity) {
        return new KillingBeginNode(locationIdentity);
    }

    @Override
    public LocationIdentity getKilledLocationIdentity() {
        return locationIdentity;
    }

    @Override
    public void prepareDelete() {
        GraalError.guarantee(graph().isBeforeStage(StructuredGraph.StageFlag.FLOATING_READS) || !hasUsagesOfType(Memory), "Cannot delete %s with memory usages %s", this, usages().snapshot());
        super.prepareDelete();
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (graph().isBeforeStage(StructuredGraph.StageFlag.FLOATING_READS)) {
            if (!(predecessor() instanceof WithExceptionNode)) {
                return new BeginNode();
            }
        } else {
            // after floating read - we need to check the memory graph
            if (tool.allUsagesAvailable() && !(predecessor() instanceof WithExceptionNode) && !hasUsagesOfType(Memory)) {
                return new BeginNode();
            }
        }
        return this;
    }
}
