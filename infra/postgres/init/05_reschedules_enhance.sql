-- Migration: enhance reschedules and reschedule_responses tables for full flow

alter table reschedules
    add column if not exists teacher_id uuid references users(id),
    add column if not exists reason text,
    add column if not exists response_deadline timestamptz;

alter table reschedules
    drop constraint if exists reschedules_status_check;

alter table reschedules
    add constraint reschedules_status_check check (status in (
        'PROPOSED', 'TEACHER_ACCEPTED', 'TEACHER_REJECTED', 'COMPLETED'
    ));

alter table reschedule_responses
    add column if not exists response_type text,
    drop constraint if exists reschedule_responses_type_check;

alter table reschedule_responses
    add constraint reschedule_responses_type_check check (response_type in (
        'ACCEPTED', 'REJECTED', 'TIMEOUT'
    ));

-- Class needs SUSPENDED status
alter table classes
    drop constraint if exists classes_status_check;

alter table classes
    add constraint classes_status_check check (status in (
        'DRAFT', 'PUBLISHED', 'FULL', 'CANCELLED', 'COMPLETED', 'SUSPENDED'
    ));
