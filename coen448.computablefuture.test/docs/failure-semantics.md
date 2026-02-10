## Overview
Failure-handling policies define how concurrent workflows respond when one or more components fail. They affect correctness, user experience, and operational risk. Choosing the right policy depends on whether the system must preserve strict consistency, can tolerate partial results, or must remain responsive even when some components are unavailable.

## Fail-Fast Policy
Fail-fast terminates the overall operation as soon as any component fails. This preserves strong correctness guarantees by preventing partial or inconsistent results.

Appropriate use cases include:
- Financial transaction processing where atomicity and integrity are required.
- Configuration management where incomplete updates could corrupt state.
- Safety-critical workflows where a single failure invalidates the outcome.

## Fail-Partial Policy
Fail-partial allows the overall operation to complete, returning only the successful results and omitting failures. This trades completeness for availability while still surfacing that some work did not succeed.

Appropriate use cases include:
- Analytics dashboards where partial data is better than no data.
- Search or recommendation systems aggregating results from multiple sources.
- Monitoring systems where missing signals are acceptable if identified.

## Fail-Soft Policy
Fail-soft completes the operation even when failures occur, substituting a fallback value in place of failures. This prioritizes continuity and predictable output shape.

Appropriate use cases include:
- User-facing applications where UI continuity is critical.
- Content aggregation with reasonable defaults or cached values.
- Non-critical personalization features that should degrade gracefully.

## Risks of Masking Failures
Fail-soft and, to a lesser extent, fail-partial can hide underlying faults. This can delay detection, skew metrics, or mislead downstream decisions. In concurrent systems, masking failures can also create false confidence in system health and complicate root cause analysis.

Key risks include:
- Silent data quality degradation in reports and dashboards.
- Hidden dependency failures that accumulate over time.
- Incorrect operational decisions based on incomplete or substituted results.

## Conclusion
Failure policies should be selected based on correctness requirements, user impact, and operational observability. Fail-fast favors integrity, fail-partial favors availability with transparency, and fail-soft favors continuity with the highest risk of masking issues. A clear policy and strong monitoring are essential in concurrent systems.