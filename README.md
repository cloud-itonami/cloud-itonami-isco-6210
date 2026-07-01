# cloud-itonami-isco-6210

Open Occupation Blueprint for **ISCO-08 6210**: Forestry and Related Workers.

This repository designs a forkable OSS business for an independent forestry operator: a forestry robot performs stand surveying and felling-support work under a governor-gated actor, so the operator keeps their own harvest and environmental-compliance records instead of renting a closed forestry SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a forestry robot performs stand surveying, felling-support and replanting assist tasks under an actor that proposes
actions and an independent **Forestry Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
felling operations, operating near workers or protected habitats) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
harvest plan + land permit + environmental constraint
        |
        v
Forestry Advisor -> Forestry Governor -> harvest/replant, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `6210`). Required capabilities:

- :robotics
- :telemetry
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/forestry/{store,governor}.cljc` is a minimal but real implementation
of the Core Contract above (pure cljc, no external deps):

- `forestry.store` — `Store` protocol + `MemStore`: stands, permits,
  fellings, replantings. A felling event can only be recorded against a
  registered permit on a registered stand (permit provenance).
- `forestry.governor` — `ForestryGovernor`: `assess` gates a proposal
  against the permit/stand env. Hard invariants force `:hold` (no permit,
  direct-write instead of `:propose`, or a felling on a `protected?` stand
  at below `:high` safety-class); felling on a protected stand always
  requires `:high`+ safety-class and thus `:human-approval` — it can never
  be auto-approved; low-confidence proposals also escalate.

```bash
clojure -M:test   # 7 tests, 13 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the 10th `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112`, `-2221`, `-7126`, `-4321`, `-9312`, `-5322`,
`-8332`, `-1321` and `-3253` (ADR-2607012000).

## License

AGPL-3.0-or-later.
