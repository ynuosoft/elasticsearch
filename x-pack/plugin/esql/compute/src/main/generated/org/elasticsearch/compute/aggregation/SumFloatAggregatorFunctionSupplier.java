// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link SumFloatAggregator}.
 * This class is generated. Edit {@code AggregatorFunctionSupplierImplementer} instead.
 */
public final class SumFloatAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  public SumFloatAggregatorFunctionSupplier() {
  }

  @Override
  public List<IntermediateStateDesc> nonGroupingIntermediateStateDesc() {
    return SumFloatAggregatorFunction.intermediateStateDesc();
  }

  @Override
  public List<IntermediateStateDesc> groupingIntermediateStateDesc() {
    return SumFloatGroupingAggregatorFunction.intermediateStateDesc();
  }

  @Override
  public SumFloatAggregatorFunction aggregator(DriverContext driverContext,
      List<Integer> channels) {
    return SumFloatAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public SumFloatGroupingAggregatorFunction groupingAggregator(DriverContext driverContext,
      List<Integer> channels) {
    return SumFloatGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "sum of floats";
  }
}
