/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.planner.plan.rules.physical.stream

import org.apache.flink.table.planner.plan.nodes.FlinkConventions
import org.apache.flink.table.planner.plan.nodes.logical.FlinkLogicalTableFunctionScan
import org.apache.flink.table.planner.plan.nodes.physical.stream.StreamPhysicalWindowTableFunction
import org.apache.flink.table.planner.plan.utils.WindowUtil
import org.apache.flink.table.planner.plan.utils.WindowUtil.convertToWindowingStrategy

import org.apache.calcite.plan.{RelOptRule, RelOptRuleCall, RelTraitSet}
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.convert.ConverterRule
import org.apache.calcite.rex.RexCall

/**
 * Rule to convert a [[FlinkLogicalTableFunctionScan]] with window table function call
 * into a [[StreamPhysicalWindowTableFunction]].
 */
class StreamPhysicalWindowTableFunctionRule  extends ConverterRule(
  classOf[FlinkLogicalTableFunctionScan],
  FlinkConventions.LOGICAL,
  FlinkConventions.STREAM_PHYSICAL,
  "StreamPhysicalWindowTableFunctionRule") {

  override def matches(call: RelOptRuleCall): Boolean = {
    val scan: FlinkLogicalTableFunctionScan = call.rel(0)
    WindowUtil.isWindowTableFunctionCall(scan.getCall)
  }

  def convert(rel: RelNode): RelNode = {
    val scan: FlinkLogicalTableFunctionScan = rel.asInstanceOf[FlinkLogicalTableFunctionScan]
    val traitSet: RelTraitSet = rel.getTraitSet.replace(FlinkConventions.STREAM_PHYSICAL)
    val newInput = RelOptRule.convert(scan.getInput(0), FlinkConventions.STREAM_PHYSICAL)

    new StreamPhysicalWindowTableFunction(
      scan.getCluster,
      traitSet,
      newInput,
      scan.getRowType,
      convertToWindowingStrategy(scan.getCall.asInstanceOf[RexCall], newInput.getRowType),
      false
    )
  }
}

object StreamPhysicalWindowTableFunctionRule {
  val INSTANCE = new StreamPhysicalWindowTableFunctionRule
}
