// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.xpack.esql.expression.function.scalar.multivalue;

import java.lang.Override;
import java.lang.String;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.DoubleBlock;
import org.elasticsearch.compute.data.DoubleVector;
import org.elasticsearch.compute.data.IntBlock;
import org.elasticsearch.compute.operator.DriverContext;
import org.elasticsearch.compute.operator.EvalOperator;
import org.elasticsearch.search.aggregations.metrics.CompensatedSum;

/**
 * {@link EvalOperator.ExpressionEvaluator} implementation for {@link MvAvg}.
 * This class is generated. Edit {@code MvEvaluatorImplementer} instead.
 */
public final class MvAvgIntEvaluator extends AbstractMultivalueFunction.AbstractEvaluator {
  public MvAvgIntEvaluator(EvalOperator.ExpressionEvaluator field, DriverContext driverContext) {
    super(driverContext, field);
  }

  @Override
  public String name() {
    return "MvAvg";
  }

  /**
   * Evaluate blocks containing at least one multivalued field.
   */
  @Override
  public Block evalNullable(Block fieldVal) {
    IntBlock v = (IntBlock) fieldVal;
    int positionCount = v.getPositionCount();
    try (DoubleBlock.Builder builder = driverContext.blockFactory().newDoubleBlockBuilder(positionCount)) {
      CompensatedSum work = new CompensatedSum();
      for (int p = 0; p < positionCount; p++) {
        int valueCount = v.getValueCount(p);
        if (valueCount == 0) {
          builder.appendNull();
          continue;
        }
        int first = v.getFirstValueIndex(p);
        if (valueCount == 1) {
          int value = v.getInt(first);
          double result = MvAvg.single(value);
          builder.appendDouble(result);
          continue;
        }
        int end = first + valueCount;
        for (int i = first; i < end; i++) {
          int value = v.getInt(i);
          MvAvg.process(work, value);
        }
        double result = MvAvg.finish(work, valueCount);
        builder.appendDouble(result);
      }
      return builder.build();
    }
  }

  /**
   * Evaluate blocks containing at least one multivalued field.
   */
  @Override
  public Block evalNotNullable(Block fieldVal) {
    IntBlock v = (IntBlock) fieldVal;
    int positionCount = v.getPositionCount();
    try (DoubleVector.FixedBuilder builder = driverContext.blockFactory().newDoubleVectorFixedBuilder(positionCount)) {
      CompensatedSum work = new CompensatedSum();
      for (int p = 0; p < positionCount; p++) {
        int valueCount = v.getValueCount(p);
        int first = v.getFirstValueIndex(p);
        if (valueCount == 1) {
          int value = v.getInt(first);
          double result = MvAvg.single(value);
          builder.appendDouble(result);
          continue;
        }
        int end = first + valueCount;
        for (int i = first; i < end; i++) {
          int value = v.getInt(i);
          MvAvg.process(work, value);
        }
        double result = MvAvg.finish(work, valueCount);
        builder.appendDouble(result);
      }
      return builder.build().asBlock();
    }
  }

  /**
   * Evaluate blocks containing only single valued fields.
   */
  @Override
  public Block evalSingleValuedNullable(Block fieldVal) {
    IntBlock v = (IntBlock) fieldVal;
    int positionCount = v.getPositionCount();
    try (DoubleBlock.Builder builder = driverContext.blockFactory().newDoubleBlockBuilder(positionCount)) {
      CompensatedSum work = new CompensatedSum();
      for (int p = 0; p < positionCount; p++) {
        int valueCount = v.getValueCount(p);
        if (valueCount == 0) {
          builder.appendNull();
          continue;
        }
        assert valueCount == 1;
        int first = v.getFirstValueIndex(p);
        int value = v.getInt(first);
        double result = MvAvg.single(value);
        builder.appendDouble(result);
      }
      return builder.build();
    }
  }

  /**
   * Evaluate blocks containing only single valued fields.
   */
  @Override
  public Block evalSingleValuedNotNullable(Block fieldVal) {
    IntBlock v = (IntBlock) fieldVal;
    int positionCount = v.getPositionCount();
    try (DoubleVector.FixedBuilder builder = driverContext.blockFactory().newDoubleVectorFixedBuilder(positionCount)) {
      CompensatedSum work = new CompensatedSum();
      for (int p = 0; p < positionCount; p++) {
        int valueCount = v.getValueCount(p);
        assert valueCount == 1;
        int first = v.getFirstValueIndex(p);
        int value = v.getInt(first);
        double result = MvAvg.single(value);
        builder.appendDouble(result);
      }
      return builder.build().asBlock();
    }
  }

  public static class Factory implements EvalOperator.ExpressionEvaluator.Factory {
    private final EvalOperator.ExpressionEvaluator.Factory field;

    public Factory(EvalOperator.ExpressionEvaluator.Factory field) {
      this.field = field;
    }

    @Override
    public MvAvgIntEvaluator get(DriverContext context) {
      return new MvAvgIntEvaluator(field.get(context), context);
    }

    @Override
    public String toString() {
      return "MvAvg[field=" + field + "]";
    }
  }
}
