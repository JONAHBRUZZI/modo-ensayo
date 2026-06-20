# SUPABASE MIGRATION — EXECUTION CHECKLIST
> Phase-by-phase verification list
> Print and use during actual migration

---

## PHASE 0: PRE-STAGING PREPARATION (This Week)

### Documentation Review
- [ ] Read PLAN_MIGRACION_SUPABASE_REFINED.md (Sections 1-4)
- [ ] Read PLAN_MIGRACION_VALIDATION_REPORT.md (Executive Summary + Critical Findings)
- [ ] Team meeting: discuss risks, timeline, gate criteria
- [ ] Assign roles: Gateway Owner, On-Call Lead, Database Manager

### Infrastructure Setup
- [ ] Supabase project created + linked via `supabase link`
- [ ] Database backups: `pg_dump -Fc > backup_20260619.dump`
- [ ] Staging environment provisioned (separate Supabase project)
- [ ] Feature flags environment variables prepared (dev, staging, prod)

### Testing Preparation
- [ ] Deno installed locally (`deno --version`)
- [ ] Test database populated with sample data
- [ ] Mercado Pago test account confirmed (sandbox mode)
- [ ] E2E test tools prepared (Playwright, test browser drivers)

### Team Training
- [ ] Ops team: Supabase debugging walk-through (1h)
- [ ] Dev team: Edge Functions development workflow (1h)
- [ ] Product team: Feature flag mechanism (30m)
- [ ] Runbooks reviewed and printed

**Gate**: Engineering Lead sign-off → Proceed to Staging Phase

---

## PHASE 1: DEV ENVIRONMENT (Days 1-2)

### Deploy Infrastructure
- [ ] `supabase start` in local dev environment
- [ ] Migrations applied: `supabase db push`
- [ ] Seed data loaded: `supabase db seed`
- [ ] Edge Functions deployed (locally): `supabase functions serve`

### Unit Testing (Edge Functions)
- [ ] Run unit tests for all 13 Edge Functions:
  ```bash
  for fn in mercadopago-create-preference mercadopago-webhook create-class \
             assign-reserva propose-reschedule teacher-decision student-decision \
             register-venue admin-approve-venue confirm-class generate-blocks \
             admin-stats create-review; do
    echo "Testing $fn..."
    cd supabase/functions/$fn
    deno test --allow-env __tests__/unit.test.ts
    cd ../../../
  done
  ```
- [ ] All tests pass (0 failures)
- [ ] Coverage reports generated

### API Testing (Manual)
- [ ] Test class creation (with/without room assignment)
- [ ] Test payment preference creation (mock Mercado Pago response)
- [ ] Test user authentication (login, signup)
- [ ] Test venue registration workflow
- [ ] Test reschedule proposal + decisions

### Verify Logs
- [ ] Edge Functions emit structured JSON logs
- [ ] Sample logs reviewed: verify fields (timestamp, event, userId, etc.)
- [ ] No console.log statements (use logging utilities)

**Gate**: All unit tests pass, manual API tests pass → Proceed to Staging Phase

---

## PHASE 2: STAGING — BOTH SYSTEMS (Days 3-4)

### Environment Setup
- [ ] Feature flags set:
  ```bash
  VITE_MIGRATE_AUTH=false
  VITE_MIGRATE_CLASSES=false
  VITE_MIGRATE_PAYMENTS=false
  VITE_MIGRATE_VENUES=false
  VITE_API_LEGACY_URL=http://backend:8080
  VITE_SUPABASE_URL=https://xxx.supabase.co
  ```
- [ ] Frontend built and deployed
- [ ] Spring Boot backend running (prod-like)
- [ ] Supabase staging project fully initialized

### Integration Testing
- [ ] Run full integration test suite:
  ```bash
  cd supabase/functions/mercadopago-create-preference
  deno test --allow-env --allow-net __tests__/integration.test.ts
  ```
- [ ] Test coverage for all 13 functions (integration layer)
- [ ] No errors, warnings logged but not blocking

### E2E Testing (Both Systems)
- [ ] Run E2E payment flow test (uses Playwright)
  ```bash
  npm run test:e2e
  ```
- [ ] Verify enrollment created + payment retained
- [ ] Verify idempotency (run webhook twice, only one enrollment)
- [ ] Test user signup + teacher registration flow
- [ ] Test venue registration + approval workflow

### Data Validation (Dry-Run)
- [ ] Export legacy data:
  ```bash
  pg_dump -t public.* --data-only -Fc > legacy_export.dump
  ```
- [ ] Run migration script (on staging database):
  ```bash
  psql supabase < scripts/migrate_users.sql
  psql supabase < scripts/migrate_venues.sql
  # ... etc
  ```
- [ ] Run validation script:
  ```bash
  psql supabase < scripts/validate-migration.sql
  ```
- [ ] All assertions pass (no orphans, counts match)

### Performance Analysis
- [ ] EXPLAIN ANALYZE critical queries:
  ```sql
  -- In supabase > SQL Editor
  EXPLAIN ANALYZE SELECT c.* FROM classes WHERE status='PUBLISHED' LIMIT 20;
  -- Expected: <100ms, uses index (idx_classes_status)
  ```
- [ ] Verify 5 key queries meet performance targets
- [ ] Connection pool stats checked

### Monitoring Setup
- [ ] Monitoring dashboard deployed (if available)
- [ ] Alert rules configured in Supabase
- [ ] Test alerts (simulate high error rate)
  - [ ] Error rate alert fires at >0.5%
  - [ ] Latency alert fires at p95 >300ms
  - [ ] Data inconsistency alert configured

**Gate**: All integration tests pass, E2E payment flow works, data validation passes, monitoring alerts tested → Proceed to Canary

---

## PHASE 3: STAGING — CANARY (Day 5)

### Feature Flag Rollout
- [ ] Set flag: `VITE_MIGRATE_CLASSES=true` (10% of traffic via canary sampling)
  ```typescript
  if (CANARY_MODE && Math.random() < 0.1) {
    // Use Supabase
  } else {
    // Use legacy
  }
  ```
- [ ] Deploy frontend with new flags
- [ ] Confirm feature flags are in effect (check logs)

### Canary Monitoring (4 hours)
- [ ] Monitor dashboard: error rate, latency, consistency
- [ ] **Error Rate**: must stay < 0.1% (0.1% allowed)
- [ ] **Latency**: p95 < 150ms (legacy is ~120ms)
- [ ] **Data Consistency**: row counts match every 10 minutes
- [ ] **Alerts**: if any trigger, prepare to rollback

### Metrics Collection
- [ ] Capture baseline:
  - [ ] Error rate per endpoint (compare Supabase vs legacy)
  - [ ] Latency distribution (p50, p95, p99)
  - [ ] Cache hit rates
  - [ ] Successful payments %

### Rollback Test
- [ ] Simulate Supabase failure: set `VITE_MIGRATE_CLASSES=false`
- [ ] Verify immediate fallback to Spring Boot
- [ ] Check error rate drops within 2 minutes
- [ ] No data loss occurs

**Gate**: Error rate < 0.1%, latency < 150ms, no data loss → Proceed to Staging Full

---

## PHASE 4: STAGING — FULL (Days 6-8)

### Full Supabase Migration
- [ ] Set all flags:
  ```bash
  VITE_MIGRATE_AUTH=true
  VITE_MIGRATE_CLASSES=true
  VITE_MIGRATE_PAYMENTS=true
  VITE_MIGRATE_VENUES=true
  ```
- [ ] Deploy frontend (100% traffic to Supabase, Spring Boot fallback enabled)

### End-to-End Testing
- [ ] **Student Flow**: signup → browse → enroll → payment → confirmation
  - [ ] User can sign up (identity verification)
  - [ ] Can browse classes (filter, search)
  - [ ] Can add to cart (persistence)
  - [ ] Can checkout (Mercado Pago integration)
  - [ ] Payment retained correctly
  - [ ] Enrollment created with correct status

- [ ] **Teacher Flow**: signup → create class → publish → manage → confirm
  - [ ] Can register as teacher (identity verification)
  - [ ] Can create draft classes
  - [ ] Can assign room + publish
  - [ ] Can manage students + attendance
  - [ ] Can confirm class completion (release payments)

- [ ] **Venue Flow**: signup → register venue → manage rooms → approve
  - [ ] Can register venue
  - [ ] Can add rooms with details
  - [ ] Can set schedules + block configs
  - [ ] Admin can approve venue
  - [ ] Payment for venue-managed classes works

- [ ] **Reschedule Flow**: propose → teacher decision → student decision → complete
  - [ ] Student proposes reschedule
  - [ ] Teacher accepts
  - [ ] Students get 48h timeout for confirmation
  - [ ] Payments adjust (if rejected by students)

### Stability Test (24+ hours)
- [ ] **Error Rate**: must stay < 0.1%
- [ ] **Latency**: p95 must stay < 150ms
- [ ] **Uptime**: 99.9% (zero downtime)
- [ ] **Data Consistency**: row counts stable
- [ ] **Payment Volume**: process test payments every hour (verify success)

### Load Testing
- [ ] Simulate 50 concurrent users:
  ```bash
  # Using k6 or similar
  k6 run load-test.js --vus 50 --duration 30m
  ```
- [ ] Capture metrics:
  - [ ] Average response time
  - [ ] p95 latency
  - [ ] Error rate during load
  - [ ] Database connection count
- [ ] Verify performance degrades gracefully (no crashes)

### Rollback Procedures Test
- [ ] **Immediate Rollback**:
  - [ ] Set all flags to false
  - [ ] Restart frontend
  - [ ] Verify all traffic goes to Spring Boot
  - [ ] No data loss
  - [ ] Time to execute: <5 minutes ✅

- [ ] **Selective Rollback**:
  - [ ] Set only `VITE_MIGRATE_PAYMENTS=false`
  - [ ] Keep classes, venues on Supabase
  - [ ] Verify payments go to legacy
  - [ ] Classes/venues still use Supabase
  - [ ] Time to execute: <2 minutes ✅

- [ ] **Data Rollback** (PITR):
  - [ ] Take snapshot of current DB state
  - [ ] Intentionally corrupt a record (update payment.amount to 0)
  - [ ] Restore from PITR backup
  - [ ] Verify record restored correctly
  - [ ] Time to execute: <10 minutes ✅

### Team Coordination
- [ ] On-call engineer trained and confident
- [ ] Runbooks reviewed and updated
- [ ] Escalation paths confirmed (who to call if issues)
- [ ] Product team sign-off on feature completeness

**Gate**: 24h stability verified, load test passed, rollback procedures tested, team sign-off → Proceed to Production Canary

---

## PHASE 5: PRODUCTION — CANARY (Day 11)

### Feature Flag Rollout
- [ ] Set all flags with 5% sampling:
  ```bash
  VITE_CANARY_SAMPLE_RATE=0.05  # 5% of real users
  VITE_MIGRATE_AUTH=true
  VITE_MIGRATE_CLASSES=true
  VITE_MIGRATE_PAYMENTS=true
  VITE_MIGRATE_VENUES=true
  ```
- [ ] Deploy to production (5% traffic to Supabase)
- [ ] Spring Boot still running (100% fallback available)

### Canary Monitoring (24 hours)
- [ ] **Error Rate**: must be < 0.05% (critical threshold)
  - [ ] Check every 5 minutes
  - [ ] Alert if > 0.05%
- [ ] **Latency**: p95 < 150ms
  - [ ] Check dashboard
  - [ ] Alert if > 200ms sustained for 10 minutes
- [ ] **Payment Failures**: 0% expected
  - [ ] Monitor webhook success rate
  - [ ] Verify all payments processed
- [ ] **Data Consistency**: check hourly
  - [ ] Row counts match
  - [ ] No orphaned records
- [ ] **User Complaints**: monitor support channel
  - [ ] 0 reports of "my data is gone"
  - [ ] 0 reports of double payments

### Business Metrics
- [ ] Revenue tracking (payments processed)
- [ ] User signup rate (no drops)
- [ ] Class enrollment rate (normal patterns)
- [ ] Customer support ticket volume (no spike)

### Incident Readiness
- [ ] On-call engineer available 24/7
- [ ] Slack/PagerDuty alerts active
- [ ] Runbooks printed and accessible
- [ ] Database backups verified

**Gate**: Error rate < 0.05%, 0 payment failures, data consistent, no user complaints → Decision Point

**Decision**:
- ✅ Metrics all green → Proceed to Production Cutover
- ⚠️ Metrics slightly elevated but acceptable → Extend canary 24h
- ❌ Critical issues detected → Rollback immediately

---

## PHASE 6: PRODUCTION — CUTOVER (Day 12+)

### Pre-Cutover (Hour -1)
- [ ] Spring Boot set to read-only mode:
  ```yaml
  # application.yml
  app:
    read-only-mode: true
  ```
- [ ] Database backup: `pg_dump -Fc > final_backup_20260620.dump`
- [ ] Announce maintenance window to users (if needed)
- [ ] On-call team assembled + briefed

### Cutover (Hour 0)
- [ ] Update feature flags to 100%:
  ```bash
  VITE_MIGRATE_AUTH=true
  VITE_MIGRATE_CLASSES=true
  VITE_MIGRATE_PAYMENTS=true
  VITE_MIGRATE_VENUES=true
  VITE_CANARY_SAMPLE_RATE=1.0  # 100%
  ```
- [ ] Deploy frontend
- [ ] Verify deployment successful (healthcheck)
- [ ] Announce in Slack: "Migration LIVE, monitoring closely"

### Cutover Verification (First 2 Hours)
- **Every 5 minutes**:
  - [ ] Error rate check: < 0.05%
  - [ ] Latency check: p95 < 150ms
  - [ ] Payment webhook check: all succeeding
  - [ ] Data consistency check: no orphans

- **Every 30 minutes**:
  - [ ] Review alert dashboard
  - [ ] Check support channel for complaints
  - [ ] Verify database connection health
  - [ ] Inspect Edge Function logs for errors

- **Hour 1**: If everything nominal, celebrate 🎉
- **Hour 2**: If issues arise, prepare selective rollback

### Post-Cutover Stability (24-48 Hours)
- [ ] Continuous monitoring
- [ ] Error rate trending downward or stable
- [ ] No data loss reports
- [ ] No payment issues
- [ ] User experience identical to legacy

### Spring Boot Sunset (2 Weeks Later)
- [ ] Confirm Supabase handling 100% traffic successfully
- [ ] No rollback needed during 2-week observation
- [ ] Set Spring Boot to decommission date in calendar
- [ ] Document lessons learned

**Gate**: 48-hour stability verified, zero data loss → Migration Complete ✅

---

## ROLLBACK DECISION TREE

**Use this during any phase to decide whether to rollback:**

```
Is error rate > 1%?
├─ YES → IMMEDIATE ROLLBACK (Section 9.1)
└─ NO → Continue

Is payment processing failing (> 5% failures)?
├─ YES → IMMEDIATE ROLLBACK (Section 9.1)
└─ NO → Continue

Is data corruption detected (orphan records)?
├─ YES → DATA ROLLBACK (Section 9.3, PITR restore)
└─ NO → Continue

Is latency > 300ms sustained for >10 minutes?
├─ YES → SELECTIVE ROLLBACK (Section 9.2, classes only)
└─ NO → Continue

Are users reporting missing data/double charges?
├─ YES → IMMEDIATE ROLLBACK (Section 9.1)
└─ NO → Continue monitoring

→ All conditions passed: Continue to next phase
```

---

## EMERGENCY CONTACTS

| Role | Name | Phone | Slack |
|------|------|-------|-------|
| On-Call Lead | ___ | +56 ___ | @___ |
| Database Manager | ___ | +56 ___ | @___ |
| Supabase Support | ___ | +1 ___ | supabase-support |
| Product Manager | ___ | +56 ___ | @___ |

---

## SIGNATURE & APPROVAL

**Phase**: _______________________________

**Date**: _______________________________

**Time**: _______________________________

**Team Lead**: ______________ (signature)

**On-Call Engineer**: ______________ (signature)

**Product Manager**: ______________ (signature)

---

## NOTES & OBSERVATIONS

```
[Use space below to record observations during execution]

Time | Observation | Action Taken | Owner
-----|-------------|--------------|------
     |             |              |
     |             |              |
     |             |              |
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-19  
**Print & Use During Migration**
