-- PROCEDIMIENTOS ALMACENADOS

create or replace function update_class_status()
returns trigger as $$
begin
    insert into class_status_history (class_id, previous_status, new_status, changed_by)
    values (new.id, old.status, new.status, new.teacher_id);
    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_class_status_change on classes;
create trigger trg_class_status_change
    after update of status on classes
    for each row
    execute function update_class_status();

create or replace function release_payment_after_class()
returns trigger as $$
begin
    if new.status = 'COMPLETED' then
        update payments p
        set status = 'RELEASED'
        from enrollments e
        where p.enrollment_id = e.id
          and e.class_id = new.id
          and p.status = 'RETAINED';
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_release_payment on classes;
create trigger trg_release_payment
    after update of status on classes
    for each row
    when (new.status = 'COMPLETED')
    execute function release_payment_after_class();

create or replace function check_enrollment_capacity()
returns trigger as $$
declare
    current_count int;
    max_capacity int;
begin
    select capacity into max_capacity from classes where id = new.class_id;
    select count(*) into current_count from enrollments where class_id = new.class_id;
    if current_count >= max_capacity then
        raise exception 'La clase esta llena (capacidad: %)', max_capacity;
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_check_capacity on enrollments;
create trigger trg_check_capacity
    before insert on enrollments
    for each row
    execute function check_enrollment_capacity();

create or replace function get_user_roles(user_uuid uuid)
returns table(role_name text) as $$
begin
    return query
    select r.name from roles r
    inner join user_roles ur on r.id = ur.role_id
    where ur.user_id = user_uuid;
end;
$$ language plpgsql;
